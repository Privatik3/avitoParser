package google;

import parser.Ad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColorData {
    
    public boolean isEmpty = true;

    int maxViews = 0;
    int minViews = 0;
    int maxDailyViews = 0;
    int minDailyViews = 0;
    int maxViewsTenDay = 0;
    int minViewsTenDay = 0;
    int maxMaxTenDay = 0;
    int minMaxTenDay = 0;
    int maxViewsAverageTenDay = 0;
    int minViewsAverageTenDay = 0;
    int maxViewYesterday = 0;
    int minViewYesterday = 0;

    public ColorData() { }

    public ColorData(List<Ad> ads) {
        this.isEmpty = false;

        List<Integer> viewsMax = new ArrayList<Integer>();
        List<Integer> dailyViewsMax = new ArrayList<Integer>();
        List<Integer> viewsTenDayMax = new ArrayList<Integer>();
        List<Integer> maxTenDayMax = new ArrayList<Integer>();
        List<Integer> viewsAverageTenDayMax = new ArrayList<Integer>();
        List<Integer> maxViewYesterdayMax = new ArrayList<Integer>();

        for (int i = 0; i < ads.size(); i++) {
            Ad ad = ads.get(i);
            if (isNum(ad.getViews())) {
                viewsMax.add(Integer.parseInt(ad.getViews()));
            }
            if (isNum(ad.getDailyViews())) {
                dailyViewsMax.add(Integer.parseInt(ad.getDailyViews()));
            }
            if (isNum(ad.getViewsTenDay())) {
                viewsTenDayMax.add(Integer.parseInt(ad.getViewsTenDay()));
            }
            try {
                maxViewYesterdayMax.add(Integer.parseInt(ad.getViewYesterday()));
            } catch (Exception ignore) {
            }

            Integer maxTenDay = ad.getMaxTenDay();
            if (maxTenDay != null)
                maxTenDayMax.add(maxTenDay);

            if (isNum(ad.getViewsAverageTenDay())) {
                viewsAverageTenDayMax.add(Integer.parseInt(ad.getViewsAverageTenDay()));
            }
        }

        maxViews = Collections.max(viewsMax);
        minViews = Collections.min(viewsMax);
        maxDailyViews = Collections.max(dailyViewsMax);
        minDailyViews = Collections.min(dailyViewsMax);
        maxViewsTenDay = Collections.max(viewsTenDayMax);
        minViewsTenDay = Collections.min(viewsTenDayMax);
        maxMaxTenDay = Collections.max(maxTenDayMax);
        minMaxTenDay = Collections.min(maxTenDayMax);
        maxViewsAverageTenDay = Collections.max(viewsAverageTenDayMax);
        minViewsAverageTenDay = Collections.min(viewsAverageTenDayMax);
        maxViewYesterday = Collections.max(maxViewYesterdayMax);
        minViewYesterday = Collections.min(maxViewYesterdayMax);
    }

    private static boolean isNum(String s) {
        try {
            Integer.parseInt(s.replaceAll(" ", ""));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getMaxViews() {
        return maxViews;
    }

    public int getMinViews() {
        return minViews;
    }

    public int getMaxDailyViews() {
        return maxDailyViews;
    }

    public int getMinDailyViews() {
        return minDailyViews;
    }

    public int getMaxViewsTenDay() {
        return maxViewsTenDay;
    }

    public int getMinViewsTenDay() {
        return minViewsTenDay;
    }

    public int getMaxMaxTenDay() {
        return maxMaxTenDay;
    }

    public int getMinMaxTenDay() {
        return minMaxTenDay;
    }

    public int getMaxViewsAverageTenDay() {
        return maxViewsAverageTenDay;
    }

    public int getMinViewsAverageTenDay() {
        return minViewsAverageTenDay;
    }

    public int getMaxViewYesterday() {
        return maxViewYesterday;
    }

    public int getMinViewYesterday() {
        return minViewYesterday;
    }
}
