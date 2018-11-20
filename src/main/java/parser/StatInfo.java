package parser;

import java.util.HashMap;
import java.util.TreeMap;

public class StatInfo {

    private String id;
    private String dateApplication;
    private String viewsTenDay;
    private String viewsAverageTenDay;
    private Integer maxTenDay;
    private String maxTenDate;
    private String viewYesterday = "0";

    private TreeMap<String, Integer> rawData = new TreeMap<>();

    public TreeMap<String, Integer> getRawData() {
        return rawData;
    }

    public void setRawData(TreeMap<String, Integer> rawData) {
        this.rawData = rawData;
    }

    public String getViewYesterday() {
        return viewYesterday;
    }

    public void setViewYesterday(String viewYesterday) {
        this.viewYesterday = viewYesterday;
    }

    public Integer getMaxTenDay() {
        return maxTenDay;
    }

    public void setMaxTenDay(Integer maxTenDay) {
        this.maxTenDay = maxTenDay;
    }

    public String getMaxTenDate() {
        return maxTenDate;
    }

    public void setMaxTenDate(String maxTenDate) {
        this.maxTenDate = maxTenDate;
    }

    public String getDateApplication() {
        return dateApplication;
    }

    public void setDateApplication(String dateApplication) {
        this.dateApplication = dateApplication;
    }

    public String getViewsTenDay() {
        return viewsTenDay;
    }

    public void setViewsTenDay(String viewsTenDay) {
        this.viewsTenDay = viewsTenDay;
    }

    public String getViewsAverageTenDay() {
        return viewsAverageTenDay;
    }

    public void setViewsAverageTenDay(String viewsAverageTenDay) {
        this.viewsAverageTenDay = viewsAverageTenDay;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
