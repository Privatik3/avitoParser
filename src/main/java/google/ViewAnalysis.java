package google;

import parser.Ad;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ViewAnalysis {

    ArrayList<String> tenDays = new ArrayList<>(10);

    int[] numOfNewAd = new int[10];
    int[] numOfUpAd = new int[10];
    int[] totalViewOfAd = new int[10];
    int[] avgViewOfAd = new int[10];

    // За сегодня ( По часам )
    int[] tdayNumOfNewAd = new int[24];
    int[] tdayTotalViewOfNewAd = new int[24];
    int[] tdayAvgViewOfNewAd = new int[24];

    int[] tdayNumOfUpAd = new int[24];
    int[] tdayTotalViewOfUpAd = new int[24];
    int[] tdayAvgViewOfUpAd = new int[24];

    // За вчера ( По часам )
    int[] ydayNumOfNewAd = new int[24];
    int[] ydayTotalViewOfNewAd = new int[24];
    int[] ydayAvgViewOfNewAd = new int[24];

    int[] ydayNumOfUpAd = new int[24];
    int[] ydayTotalViewOfUpAd = new int[24];
    int[] ydayAvgViewOfUpAd = new int[24];


    public ViewAnalysis(List<Ad> ads) throws ParseException {

        // Нужно придумать систему генерации даты
        Optional<Ad> hasStat = ads.stream().filter(Ad::getHasStats).findFirst();
        hasStat.ifPresent(ad -> tenDays.add(ad.getRawStatData().firstKey()));

        String startDate = tenDays.size() > 0 ? tenDays.get(0) : new SimpleDateFormat("yyyy.MM.dd").format(new Date());
        for (int i = 1; i < 10; i++) {
            Date today = new SimpleDateFormat("yyyy.MM.dd").parse(startDate);
            long newTime = today.getTime() - ((1000 * 60 * 60 * 24) * i);
            String newDate = new SimpleDateFormat("yyyy.MM.dd").format(new Date(newTime));

            tenDays.add(newDate);
        }

        for (Ad ad : ads) {
            // Количество Новых обьявлений
            int dayOfCreationIndex = tenDays.indexOf(ad.getDateApplication());
            if (dayOfCreationIndex != -1)
                numOfNewAd[dayOfCreationIndex]++;

            // Количество Апнутых обьявлений
            int dayOfUpdateIndex = tenDays.indexOf(ad.getData().replaceAll("\\s.*", ""));
            if (dayOfUpdateIndex != -1)
                numOfUpAd[dayOfUpdateIndex]++;

            for (int i = 0; i < 10; i++) {
                String day = tenDays.get(i);
                Integer val = ad.getRawStatData().get(day);
                if (val != null && val != 0) {
                    totalViewOfAd[i] += val;
                    avgViewOfAd[i]++;
                }
            }

            // Формируем почасовые таблицы
            for (int i = 0; i < 24; i++) {
                tdayNumOfNewAd[i] = 10;
                tdayTotalViewOfNewAd[i] = 10;
                tdayAvgViewOfNewAd[i] = 10;

                tdayNumOfUpAd[i] = 10;
                tdayTotalViewOfUpAd[i] = 10;
                tdayAvgViewOfUpAd[i] = 10;

                ydayNumOfNewAd[i] = 10;
                ydayTotalViewOfNewAd[i] = 10;
                ydayAvgViewOfNewAd[i] = 10;

                ydayNumOfUpAd[i] = 10;
                ydayTotalViewOfUpAd[i] = 10;
                ydayAvgViewOfUpAd[i] = 10;
            }
        }

        // Расчитываем среднее всех просмотров, по дням
        for (int i = 0; i < 10; i++) {
            avgViewOfAd[i] = totalViewOfAd[i] / avgViewOfAd[i];
        }
    }

    public ArrayList<String> getTenDays() {
        return tenDays;
    }

    public int[] getNumOfNewAd() {
        return numOfNewAd;
    }

    public int[] getNumOfUpAd() {
        return numOfUpAd;
    }

    public int[] getTotalViewOfAd() {
        return totalViewOfAd;
    }

    public int[] getAvgViewOfAd() {
        return avgViewOfAd;
    }
}
