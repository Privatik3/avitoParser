package parser;

import manager.RequestTask;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

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
            Document doc = Jsoup.parse(task.getHtml());

            ItemInfo item = new ItemInfo();
            item.setId(task.getId());
            // Здесь гавнярит Александр

            String title = "";
            try {
                title = doc.select(".title-info-title-text").text();
            }catch (Exception ignored) {}
            item.setTitle(title);

            String price = "";
            try {
                price = doc.select(".item-view-right #price-value .js-item-price").text();
            }catch (Exception ignored) {}
            item.setPrice(price);

            String views = "";
            try {
                views = doc.select(".title-info-views").text();
               views = views.split("\\(")[0];
            }catch (Exception ignored) {}
            item.setViews(views);

            String dailyViews = "";
            try {
                dailyViews = doc.select(".title-info-views").text();
               dailyViews = dailyViews.split("\\(\\+")[1];
               dailyViews = dailyViews.split("\\)")[0];
            }catch (Exception ignored) {}
            item.setDailyViews(dailyViews);

            String address = "";
            try {
                Elements select = doc.select(".seller-info-label");
                for (Element el : select) {
                    if (el.text().contains("Адрес")) {
                        address = el.parent().select(".seller-info-value").text();
                    }
                }
            }catch (Exception ignored) {}
            item.setAddress(address);

            String data = "";
            try {
                data = doc.select(".title-info-metadata-item").get(0).text();
                data = data.split("размещено ")[1];
                SimpleDateFormat format = new SimpleDateFormat("dd MMMMM " , new Locale("ru"));
                final Date now = new Date();
                if (data.contains("сегодня")) {
                    data = format.format(now) + data.substring(data.indexOf("в "));
                } else if (data.contains("вчера")) {
                    data = format.format(new Date(now.getTime() - 60 * 60 * 24 * 1000)) + data.substring(data.indexOf("в "));
                }
                    String day = data.split(" ")[0];
                    String monthData = data.split(" ")[1];
                    String time = data.split(" ")[3];
                    try {
                        switch (monthData) {
                            case "января": monthData = "01";break;case "февраля": monthData = "02";break;case "марта": monthData = "03";break;case "апреля": monthData = "04";break;case "мая": monthData = "05";break;case "июня": monthData = "06";break;case "июля": monthData = "07";break;case "августа": monthData = "08";break;case "сентября": monthData = "09";break;case "октября": monthData = "10";break;case "ноября": monthData = "11";break;case "декабря": monthData = "12";break;
                        }
                    } catch (Exception ignored) {
                    }
                    data = monthData + "-" + day + "-" + time;
            }catch (Exception ignored) {}
            item.setData(data);

            String numberPictures = "";
            try {
               Elements numberPicturesEl = doc.select(".gallery-list-wrapper li");
                numberPictures = String.valueOf(numberPicturesEl.size());
            }catch (Exception ignored) {}
            item.setNumberPictures(numberPictures);

            String text = "";
            String quantityText = "";
            try {
                text = doc.select(".item-description div").text();
                quantityText = String.valueOf(text.length());
            }catch (Exception ignored) {}
            item.setText(text);
            item.setQuantityText(quantityText);

            String seller = "";
            String sellerId = "";
            try {
                seller = doc.select(".item-view-right .seller-info-name a").get(0).text();
                if (seller.isEmpty()) {
                    seller = doc.select(".seller-info-prop_short_margin .seller-info-value").get(0).text();
                }
                sellerId = doc.select(".item-view-right .seller-info-name a").attr("href");
                if (sellerId.contains("id=")) {
                    sellerId = sellerId.split("id=")[1];
                    sellerId = sellerId.split("&")[0];
                }
                if (sellerId.contains("Id=")) {
                    sellerId = sellerId.split("Id=")[1];
                }
            }catch (Exception ignored) {}
            item.setSeller(seller);
            item.setSellerId(sellerId);


            // Здесь уже норм код
            result.add(item);
        }

        log.info("Время затраченое на обработку: " + (new Date().getTime() - start) + " ms");
        return result;
    }
}
