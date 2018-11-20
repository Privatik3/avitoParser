package google;

import parser.Ad;

import java.util.List;

public class CompetitorAnalysis {

    private String sellerTitle = "";
    private String phone = "";
    private Integer adCount = 0;
    private Double position = 0.0;
    private Integer promAd = 0;
    private String promMethods = "";
    private String maxViewDate = "";
    private Integer totalActiveAd = 0;
    private Integer totalView = 0;
    private Integer todayView = 0;
    private Integer yesterdayView = 0;
    private Integer totalTenDaysView = 0;
    private Integer maxTenDaysView = 0;
    private Double photo = 0.0;
    private Double textCount = 0.0;
    private Integer delivery = 0;

    public CompetitorAnalysis(List<Ad> sellerAds) {


    }

    public String getSellerTitle() {
        return sellerTitle;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getAdCount() {
        return adCount;
    }

    public Double getPosition() {
        return position;
    }

    public Integer getPromAd() {
        return promAd;
    }

    public String getPromMethods() {
        return promMethods;
    }

    public String getMaxViewDate() {
        return maxViewDate;
    }

    public Integer getTotalActiveAd() {
        return totalActiveAd;
    }

    public Integer getTotalView() {
        return totalView;
    }

    public Integer getTodayView() {
        return todayView;
    }

    public Integer getYesterdayView() {
        return yesterdayView;
    }

    public Integer getTotalTenDaysView() {
        return totalTenDaysView;
    }

    public Integer getMaxTenDaysView() {
        return maxTenDaysView;
    }

    public Double getPhoto() {
        return photo;
    }

    public Double getTextCount() {
        return textCount;
    }

    public Integer getDelivery() {
        return delivery;
    }
}
