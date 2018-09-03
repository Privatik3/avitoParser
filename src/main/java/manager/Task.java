package manager;

import api.History;
import api.RecordType;
import db.DBHandler;
import google.ReportFilter;
import google.SheetsExample;
import manager.exeption.ZeroResultException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import parser.*;
import socket.EventSocket;
import utility.RequestManager;

import java.net.URL;
import java.text.SimpleDateFormat;
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

    public Task(String token, HashMap<String, ArrayList<String>> params) throws Exception {
        this.token = token;

        if (params.containsKey("ip")) {
            ip = params.get("ip").get(0).toLowerCase();
            params.remove("ip");
        }

        if (params.containsKey("title")) {
            title = params.get("title").get(0);
            params.remove("title");
        }

        reportFilter = getReportFilters(params);
        checkCountOfAds(params);
        reqTasks = initTasks(params);
    }

    private void checkCountOfAds(HashMap<String, ArrayList<String>> initParams) throws Exception {

        try {
            HashMap<String, ArrayList<String>> params = new HashMap<>(initParams);
            params.computeIfPresent("bt", (key, value) -> new ArrayList<>(Collections.singletonList("1")));
            params.computeIfPresent("i", (key, value) -> new ArrayList<>(Collections.singletonList("1")));
            params.computeIfPresent("d", (key, value) -> new ArrayList<>(Collections.singletonList("1")));

            params.remove("max_pages");
            params.remove("s");
            params.remove("user");

            StringBuilder reqUrl = new StringBuilder("https://www.avito.ru/js/catalog?");
            params.computeIfAbsent("_", k -> new ArrayList<>()).add("6");
            params.computeIfAbsent("countOnly", k -> new ArrayList<>()).add("1");

            for (Map.Entry<String, ArrayList<String>> param : params.entrySet()) {
                String name = param.getKey();
                for (String value : param.getValue())
                    reqUrl.append(String.format("%s=%s&", name, value));
            }

            String url = reqUrl.toString().substring(0, reqUrl.length() - 1);

            String genreJson = Jsoup.connect(url)
                    .timeout(10 * 1000)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; rv:40.0) Gecko/20100101 Firefox/40.0")
                    .method(Connection.Method.GET)
                    .get()
                    .body()
                    .text();

            JSONObject json = new JSONObject(genreJson);
            int count = json.getInt("count");
            log.info("Инициализирован запрос по токену: " + token + ". Количество обьявлений: " + count);
            if (count == 0) throw new ZeroResultException();
        } catch (ZeroResultException e) {
            throw new Exception("По вашему запросу было получено 0 обьявлений");
        } catch (Exception e) {
            throw new Exception("Ошибка во время создание запроса, попробуйте позже");
        }
    }

    private ReportFilter getReportFilters(HashMap<String, ArrayList<String>> params) {
        ReportFilter result = new ReportFilter();

        result.setPhoto(params.containsKey("photo"));
        params.remove("photo");

        result.setDescription(params.containsKey("description"));
        params.remove("description");

        result.setDescriptionLength(params.containsKey("descriptionLength"));
        params.remove("descriptionLength");

        result.setSellerName(params.containsKey("sellerName"));
        params.remove("sellerName");

        result.setPosition(params.containsKey("position"));
        params.remove("position");

        result.setDate(params.containsKey("date"));
        params.remove("date");

        result.setPhone(params.containsKey("phone"));
        params.remove("phone");

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
                log.info("-------------------------------------------------");
                log.info("Выполняем запрос на выкачку страниц статистики");
                for (Ad ad : result) {
                    if (ad.hasStats() == null || !ad.hasStats()) continue;

                    String url = "https://www.avito.ru/items/stat/" + ad.getId() + "?step=0";
                    reqTasks.add(new RequestTask(ad.getId(), url, ReqTaskType.STATS));
                }

                List<StatInfo> statInfo = new ArrayList<>();
                do {
                    EventSocket.checkToken(token);

                    RequestManager.closeClient();
                    List<RequestTask> statsHtml = RequestManager.execute(token, new ArrayList<>(reqTasks));

                    statInfo.addAll(AvitoParser.parseStats(statsHtml));
                    reqTasks.removeIf(t -> statsHtml.stream().anyMatch(s -> s.getId().equals(t.getId())));
                    statsHtml.clear();
                } while (reqTasks.size() > 100);
                reqTasks.clear();



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

            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(title.substring(title.lastIndexOf("|") + 2));
            History record = new History(
                    ip, token, result.size(), endTime, title.replaceAll("\\s\\|\\s\\d+-.*$", ""),
                    resultLink, date, RecordType.GOOGLE_DOCS);
            DBHandler.saveHistory(record);
            result.clear();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ошибка во время парсинга");
            log.log(Level.SEVERE, "Exception: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Ошибка во время парсинга");
        }
    }

    private ArrayList<RequestTask> initTasks(HashMap<String, ArrayList<String>> params) throws Exception {

        ArrayList<RequestTask> result = new ArrayList<>();

        try {
            int pages = Integer.parseInt(params.get("max_pages").get(0));
            params.remove("max_pages");

            params.computeIfPresent("name", (key, value) ->
                    new ArrayList<>(Collections.singletonList(value.get(0).replace(" ", "+"))));
            Connection.Response res = Jsoup.connect("https://www.avito.ru/search")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0")
                    .followRedirects(true)
                    .data(convertToData(params))
                    .method(Connection.Method.POST)
                    .execute();

            int parsePages = 1;
            Document doc = res.parse();

            System.out.println(doc.baseUri());
            Elements linkCount = doc.select("span.breadcrumbs-link-count");
            if (linkCount.size() > 0) {
                try {
                    int allAds = Integer.parseInt(linkCount.get(0).text().replace(" ", ""));
                    if (allAds > 50)
                        parsePages = (allAds / 50) % 50 == 0 ? allAds / 50 : (allAds / 50) + 1;
                } catch (Exception ignored) { }
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
        } catch (Exception e) {
            throw new Exception("Ошибка во время создание запроса, попробуйте позже");
        }

        return result;
    }

    private Collection<Connection.KeyVal> convertToData(HashMap<String, ArrayList<String>> params) {

        ArrayList<Connection.KeyVal> result = new ArrayList<>();

        for (Map.Entry<String, ArrayList<String>> param : params.entrySet()) {
            String name = param.getKey();
            for (String value : param.getValue())
                result.add(HttpConnection.KeyVal.create(name, value));
        }

        return result;
    }
}
