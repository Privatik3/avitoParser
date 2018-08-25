package manager;

import api.History;
import db.DBHandler;
import google.ReportFilter;
import google.SheetsExample;
import manager.exeption.ZeroResultException;
import org.eclipse.jetty.util.UrlEncoded;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import parser.*;
import socket.EventSocket;
import utility.RequestManager;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Task {

    private static Logger log = Logger.getLogger("");

    private String token;
    private String title = "";
    private String ip = "";

    private String resultLink;
    private ArrayList<RequestTask> reqTasks;
    private List<Ad> result = new ArrayList<>();
    private ReportFilter reportFilter;

    public Task(String token, HashMap<String, String> param) throws IOException {
        this.token = token;

        if (param.containsKey("ip")) {
            ip = param.get("ip").toLowerCase();
            param.remove("ip");
        }

        if (param.containsKey("title")) {
            title = param.get("title");
            param.remove("title");
        }

        param.entrySet().removeIf(next -> next.getValue().isEmpty());
        reportFilter = getReportFilters(param);
        reqTasks = initTasks(param);
    }

    private ReportFilter getReportFilters(HashMap<String, String> param) {
        ReportFilter result = new ReportFilter();

        result.setPhoto(param.containsKey("photo"));
        param.remove("photo");

        result.setDescription(param.containsKey("description"));
        param.remove("description");

        result.setDescriptionLength(param.containsKey("descriptionLength"));
        param.remove("descriptionLength");

        result.setSellerName(param.containsKey("sellerName"));
        param.remove("sellerName");

        result.setPosition(param.containsKey("position"));
        param.remove("position");

        result.setDate(param.containsKey("date"));
        param.remove("date");

        result.setPhone(param.containsKey("phone"));
        param.remove("phone");

        return result;
    }

    public String getToken() {
        return token;
    }

    public String getResultLink() {
        return resultLink;
    }

    public void start() throws Exception {
        long startTime = new Date().getTime();
        try {
            EventSocket.checkToken(token);
            if (reqTasks.size() == 0)
                throw new ZeroResultException("Получено 0 обьявлений, попробуйте другой запрос");

            log.info("Выполняем запрос на выкачку страниц каталога");
            List<RequestTask> pagesHtml = RequestManager.execute(token, reqTasks);
            reqTasks.clear();

            List<PageInfo> pageInfo = AvitoParser.parsePages(pagesHtml);
            pagesHtml.clear();

            log.info("-------------------------------------------------");
            log.info("Формируем начальный список результатов");
            for (PageInfo info : pageInfo) {
                result.add(new Ad(info));
                reqTasks.add(new RequestTask(info.getId(), info.getUrl().replace("www", "m"), ReqTaskType.ITEM));
            }
            pageInfo.clear();
            if (reqTasks.size() == 0)
                throw new ZeroResultException("Получено 0 обьявлений, попробуйте другой запрос");

            EventSocket.checkToken(token);
            log.info("-------------------------------------------------");
            log.info("Выполняем запрос на выкачку страниц обьявлений");
            List<RequestTask> itemsHtml = RequestManager.execute(token, reqTasks);
            reqTasks.clear();

            List<ItemInfo> itemInfo = AvitoParser.parseItems(itemsHtml);
            itemsHtml.clear();

            log.info("-------------------------------------------------");
            log.info("Дополняем результат информацией полечунной со страниц обьявления");
            for (ItemInfo info : itemInfo) {
                String infoID = info.getId();

                Optional<Ad> first = result.stream().filter(itm -> itm.getId().equals(infoID)).findFirst();
                first.ifPresent(ad -> ad.addPageInfo(info));
            }
            itemInfo.clear();

            if (reportFilter.isDate()) {
                RequestManager.closeClient();

                log.info("-------------------------------------------------");
                log.info("Выполняем запрос на выкачку страниц статистики");
                for (Ad ad : result) {
                    if (ad.hasStats() == null || !ad.hasStats()) continue;

                    String url = "https://www.avito.ru/items/stat/" + ad.getId() + "?step=0";
                    reqTasks.add(new RequestTask(ad.getId(), url, ReqTaskType.STATS));
                }

                EventSocket.checkToken(token);
                List<RequestTask> statsHtml = RequestManager.execute(token, reqTasks);
                reqTasks.clear();

                List<StatInfo> statInfo = AvitoParser.parseStats(statsHtml);
                statsHtml.clear();

                log.info("-------------------------------------------------");
                log.info("Дополняем результат, статистекой просмотров");
                for (StatInfo info : statInfo) {
                    String infoID = info.getId();

                    Optional<Ad> first = result.stream().filter(itm -> itm.getId().equals(infoID)).findFirst();
                    first.ifPresent(ad -> ad.addStatInfo(info));
                }
                statInfo.clear();
            }

            int endTime = (int) (new Date().getTime() - (startTime));
            log.info("-------------------------------------------------");
            log.info("ПОЛНОЕ ВРЕМЯ ВЫПОЛНЕНИЯ: " + endTime + " ms");
            log.info("-------------------------------------------------");

            EventSocket.checkToken(token);
            RequestManager.closeClient();

            this.resultLink = SheetsExample.generateSheet(title, result, reportFilter);
            EventSocket.sendResult(this);

            // API
            log.info("-------------------------------------------------");
            log.info("Сохраняем историю запроса в базу");
            DBHandler.saveHistory(new History(ip, token, title, result.size(), endTime, resultLink));
            result.clear();
        } catch (ZeroResultException zeroEx) {
            throw zeroEx;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ошибка во время парсинга");
            log.log(Level.SEVERE, "Exception: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Ошибка во время парсинга");
        }
    }

    private ArrayList<RequestTask> initTasks(HashMap<String, String> parameters) throws IOException {

        ArrayList<RequestTask> result = new ArrayList<>();

        int pages = Integer.parseInt(parameters.get("max_pages"));
        parameters.remove("max_pages");

        if (parameters.containsKey("name"))
            parameters.put("name", parameters.get("name").replace(" ", "+"));

        Connection.Response res = Jsoup.connect("https://www.avito.ru/search")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0")
                .followRedirects(true)
                .data(parameters)
                .method(Connection.Method.POST)
                .execute();

        int parsePages = 1;
        Document doc = res.parse();

        Elements linkCount = doc.select("span.breadcrumbs-link-count");
        if (linkCount.size() > 0) {
            try {
                int allAds = Integer.parseInt(linkCount.get(0).text().replace(" ", ""));
                if (allAds > 50)
                    parsePages = allAds / 50;
            } catch (Exception ignored) {
            }
        } else {
            Elements pagination = doc.select("div.pagination-pages a");
            if (!pagination.isEmpty()) {
                Element last = pagination.get(pagination.size() - 1);
                String lastHref = last.attr("href");

                parsePages = Integer.parseInt(
                        lastHref.substring(lastHref.indexOf("p=") + 2,
                                lastHref.contains("&") ? lastHref.indexOf("&") : lastHref.length()));
            }
        }

        pages = parsePages > pages ? pages : parsePages;
        URL url = res.url();

        for (int i = 1; i <= pages; i++) {
            RequestTask page = new RequestTask(
                    String.valueOf(i), url + (url.toString().contains("?") ? "&" : "?") + "p=" + i, ReqTaskType.CATEGORY);
            result.add(page);
        }

        return result;
    }
}
