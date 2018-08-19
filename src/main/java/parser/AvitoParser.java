package parser;

import manager.RequestTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class AvitoParser {

    private static Logger log = Logger.getLogger(AvitoParser.class.getName());

    public static List<PageInfo> parsePages(List<RequestTask> tasks) {

        log.info("-------------------------------------------------");
        log.info("Начинаем обработку страниц каталога");

        long start = new Date().getTime();

        ArrayList<PageInfo> result = new ArrayList<>();
        for (RequestTask task : tasks) {
            Document doc = Jsoup.parse(task.getHtml());

            for (Element el : doc.select("div.item")) {
                PageInfo item = new PageInfo();
                // Здесь гавнярит Александр


                // Здесь уже норм код
                result.add(item);
            }
        }

        log.info("Время затраченое на обработку: " + (new Date().getTime() - start) + " ms");
        return result;
    }

    public static List<ItemInfo> parseItems(List<RequestTask> tasks) {

        log.info("-------------------------------------------------");
        log.info("Начинаем обработку страниц обьявлений");

        long start = new Date().getTime();

        ArrayList<ItemInfo> result = new ArrayList<>();
        for (RequestTask task : tasks) {
            Document doc = Jsoup.parse(task.getHtml());

            ItemInfo item = new ItemInfo();
            item.setId(task.getId());
            // Здесь гавнярит Александр

            
            
            // Здесь уже норм код
            result.add(item);
        }

        log.info("Время затраченое на обработку: " + (new Date().getTime() - start) + " ms");
        return result;
    }
}
