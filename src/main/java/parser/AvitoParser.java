package parser;

import com.fasterxml.jackson.core.JsonParser;
import com.sun.xml.internal.bind.v2.TODO;
import jdk.nashorn.internal.parser.JSONParser;
import manager.RequestTask;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvitoParser {

    private static Logger log = Logger.getLogger(AvitoParser.class.getName());

    public static List<PageInfo> parsePages(List<RequestTask> tasks) {

        log.info("-------------------------------------------------");
        log.info("Начинаем обработку страниц каталога");

        long start = new Date().getTime();
        tasks.sort((t1, t2) -> Integer.parseInt(t1.getId()) > Integer.parseInt(t2.getId()) ? 1 : -1);

        int itemPosition = 1;
        ArrayList<PageInfo> result = new ArrayList<>();
        for (RequestTask task : tasks) {
            Document doc = Jsoup.parse(task.getHtml());

            for (Element el : doc.select("div.item")) {
                PageInfo item = new PageInfo();
                item.setPosition(itemPosition++);
                // Здесь гавнярит Александр

                String id = "";
                try {
                    id = el.attr("data-item-id");
                }catch (Exception ignored) {}
                item.setId(id);

                String finalId = id;
                if (result.stream().anyMatch(e -> e.getId().equals(finalId))) {
                    itemPosition--;
                    continue;
                }

                String url = "";
                String urlFull = "";
                try {
                    url = el.select(".item-photo a").attr("href");
                    urlFull = "https://www.avito.ru" + url;
                }catch (Exception ignored) {}
                item.setUrl(urlFull);

                boolean isOnlyUpped = false;
                boolean isLessTwoVAS = false;
                boolean isPremium = false;
                boolean isVip = false;
                boolean isUrgent = false;
                boolean isUpped = false;
                try {
                    Elements select = el.select("div.vas-applied_bottom i");
                    if (select.size() > 0) {
                        String genreJson = select.attr("data-config");
                        JSONObject json = new JSONObject(genreJson);
                        try {
                            isPremium = json.getBoolean("isPremium");
                        }catch (Exception ignored) {}
                        try {
                            isOnlyUpped = json.getBoolean("isOnlyUpped");
                        }catch (Exception ignored) {}
                        try {
                            isLessTwoVAS = json.getBoolean("isLessTwoVAS");
                        }catch (Exception ignored) {}
                        try {
                            isVip = json.getBoolean("isVip");
                        }catch (Exception ignored) {}
                        try {
                            isUrgent = json.getBoolean("isUrgent");
                        }catch (Exception ignored) {}
                        try {
                            isUpped = json.getBoolean("isUpped");
                        }catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                item.setPremium(isPremium);
                item.setOnlyUpped(isOnlyUpped);
                item.setLessTwoVAS(isLessTwoVAS);
                item.setVip(isVip);
                item.setUrgent(isUrgent);
                item.setUpped(isUpped);



//                item.setId();
//                item.setUrl();

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
//            Document doc = Jsoup.parse(task.getHtml());

            Pattern p = Pattern.compile("window.__initialData__ =\\s(.*)\\s\\|\\|");
            Matcher m = p.matcher(task.getHtml());

            String json;
            if (m.find())
                json = m.group(1);
            else
                continue;

            JSONObject ob = new JSONObject(json);

            ItemInfo item = new ItemInfo();
            item.setId(task.getId());
            // Здесь гавнярит Александр

            String title = "";
            try {
                title = ob.getJSONObject("item").getJSONObject("currentItem").getString("title");
            } catch (Exception ignored) {}
            item.setTitle(title);

            String price = "";
            try {
                price = ob.getJSONObject("item").getJSONObject("currentItem").getJSONObject("price").getString("value");
            } catch (Exception ignored) {}
            item.setPrice(price);

            int views = 0;
            try {
                views = ob.getJSONObject("item").getJSONObject("currentItem").getJSONObject("stats").getJSONObject("views").getInt("total");
            } catch (Exception ignored) {}
            item.setViews(String.valueOf(views));

            int dailyViews = 0;
            try {
                dailyViews = ob.getJSONObject("item").getJSONObject("currentItem").getJSONObject("stats").getJSONObject("views").getInt("today");
            } catch (Exception ignored) {}
            item.setDailyViews(String.valueOf(dailyViews));

            String address = "";
            try {
                address = ob.getJSONObject("item").getJSONObject("currentItem").getString("address");
            } catch (Exception ignored) {}
            item.setAddress(address);

            String dataNew = "";
            try {
                long data = ob.getJSONObject("item").getJSONObject("currentItem").getLong("time");
                dataNew = new SimpleDateFormat("yyyy.MM.dd HH:mm").format(new Date(data * 1000L));
            } catch (Exception ignored) {}
            item.setData(String.valueOf(dataNew));

            String numberPictures = "";
            try {
                numberPictures = String.valueOf(ob.getJSONObject("item").getJSONObject("currentItem").getJSONArray("images").length());
            } catch (Exception ignored) {}
            item.setNumberPictures(numberPictures);

            String description = "";
            String quantityText = "";
            try {
                description = ob.getJSONObject("item").getJSONObject("currentItem").getString("description").replaceAll("\n", " ");
                quantityText = String.valueOf(description.length());
            } catch (Exception ignored) {}
            item.setText(description);
            item.setQuantityText(quantityText);

            String seller = "";
            try {
                seller = ob.getJSONObject("item").getJSONObject("currentItem").getJSONObject("seller").getString("name");
            } catch (Exception ignored) {}
            item.setSeller(seller);

            int sellerId = 0;
            try {
                sellerId = ob.getJSONObject("item").getJSONObject("currentItem").getInt("id");
            }catch (Exception ignored) {}
            item.setSellerId(String.valueOf(sellerId));

            String phone = "";
            try {
                JSONObject contactList = ob.getJSONObject("item").getJSONObject("currentItem").getJSONObject("contacts").getJSONArray("list").getJSONObject(0);
                phone = contactList.getJSONObject("value").getString("uri");
                phone = URLDecoder.decode(phone.substring(phone.lastIndexOf("=") + 1));
            } catch (Exception ignored) {}
            item.setPhone(phone);

            Boolean hasStats = true;
            try {
            if (views == dailyViews) {
                hasStats = false;
            }
            }catch (Exception ignored) {}
            item.setHasStats(hasStats);

            // Здесь уже норм код
            result.add(item);
        }

        log.info("Время затраченое на обработку: " + (new Date().getTime() - start) + " ms");
        return result;
    }
}
