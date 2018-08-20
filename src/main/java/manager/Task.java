package manager;

import db.DBHandler;
import google.ReportFilter;
import google.SheetsExample;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import parser.Ad;
import parser.AvitoParser;
import parser.ItemInfo;
import parser.PageInfo;
import utility.RequestManager;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class Task {

    private static Logger log = Logger.getLogger("");
    private static Boolean DEBUG_MODE = true;

    private String token;
    private String resultLink;
    private HashMap<String, String> param;
    private String title = "";
    private String ip = "";
    private ArrayList<RequestTask> reqTasks;
    private List<Ad> resultList = new ArrayList<>();
    private ReportFilter reportFilter;

    public Task(String token, HashMap<String, String> param) throws IOException {
        this.token = token;
        this.param = param;

        if (param.containsKey("ip")) {
            ip = param.get("ip").toLowerCase();
            param.remove("ip");
        }

        if (param.containsKey("title")) {
            title = param.get("title");
            param.remove("title");
        }

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

        result.setPhoto(param.containsKey("phone"));
        param.remove("phone");

        return result;
    }

    public String getToken() {
        return token;
    }

    public String getResultLink() {
        return resultLink;
    }

    public void start() {

        try {
            long startTime = new Date().getTime();

            log.info("Выполняем запрос на выкачку страниц каталога");
            List<RequestTask> pagesHtml = RequestManager.execute(reqTasks, DEBUG_MODE);
            reqTasks.clear();

            List<PageInfo> pageInfo = AvitoParser.parsePages(pagesHtml);
            pagesHtml.clear();

            log.info("-------------------------------------------------");
            log.info("Формируем начальный список результатов");
            for (PageInfo info : pageInfo) {
                resultList.add(new Ad(info));

                reqTasks.add(new RequestTask(info.getId(), info.getUrl(), ReqTaskType.ITEM));
            }
            pageInfo.clear();

            log.info("-------------------------------------------------");
            log.info("Выполняем запрос на выкачку страниц обьявлений");
            List<RequestTask> itemsHtml = RequestManager.execute(reqTasks, DEBUG_MODE);
            reqTasks.clear();

            List<ItemInfo> itemInfo = AvitoParser.parseItems(itemsHtml);
            itemsHtml.clear();

            log.info("-------------------------------------------------");
            log.info("Дополняем результат информацией полечунной со страниц обьявления");
            for (ItemInfo info : itemInfo) {
                String infoID = info.getId();

                Optional<Ad> first = resultList.stream().filter(itm -> itm.getId().equals(infoID)).findFirst();
                first.ifPresent(ad -> ad.addPageInfo(info));
            }
            itemInfo.clear();

            int endTime = (int) (new Date().getTime() - (startTime));
            log.info("-------------------------------------------------");
            log.info("ПОЛНОЕ ВРЕМЯ ВЫПОЛНЕНИЯ: " + endTime + " ms");
            log.info("-------------------------------------------------");

            RequestManager.closeClient();
            DBHandler.close();
            this.resultLink = SheetsExample.generateSheet(title, resultList, reportFilter);

            // API
//            DbManager.saveHistory(new History(ip, token, title, result.size(), endTime, resultLink));

            for (Ad ad : resultList)
                System.out.println(ad);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<RequestTask> initTasks(HashMap<String, String> parameters) throws IOException {

        ArrayList<RequestTask> result = new ArrayList<>();

        if (DEBUG_MODE) {
            result.add(new RequestTask("1", "debug", ReqTaskType.CATEGORY));
            return result;
        }

        int pages = Integer.parseInt(parameters.get("max_pages"));
        parameters.remove("max_pages");

        Connection.Response res = Jsoup.connect("https://www.avito.ru/search")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0")
                .followRedirects(true)
                .data(parameters)
                .method(Connection.Method.POST)
                .execute();

        int parsePages = 1;
        Document doc = res.parse();
        Elements pagination = doc.select("div.pagination-pages a");
        if (!pagination.isEmpty()) {
            Element last = pagination.get(pagination.size() - 1);
            String lastHref = last.attr("href");

            parsePages = Integer.parseInt(
                    lastHref.substring(lastHref.indexOf("p=") + 2,
                    lastHref.contains("&") ? lastHref.indexOf("&") : lastHref.length()));
        }

        pages = pages < parsePages ? pages : parsePages;
        URL url = res.url();

        for (int i = 1; i <= pages; i++) {
            RequestTask page = new RequestTask(String.valueOf(i), url + "&p=" + i, ReqTaskType.CATEGORY);
            result.add(page);
        }

        return result;
    }
}
