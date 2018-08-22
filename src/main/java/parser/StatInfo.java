package parser;

public class StatInfo {

    private String id;
    private String dateApplication;
    private String viewsTenDay;
    private String viewsAverageTenDay;

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
