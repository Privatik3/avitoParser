package google;

import parser.Ad;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        this.adCount = sellerAds.size();
        this.phone = sellerAds.get(0).getPhone();
        this.sellerTitle = sellerAds.get(0).getTitle();
        this.totalActiveAd = Integer.parseInt(sellerAds.get(0).getActiveAd());

        String today = "########";
        Optional<Ad> any = sellerAds.stream().filter(ad -> ad.getRawStatData().size() > 0).findAny();
        if (any.isPresent())
            today = any.get().getRawStatData().firstKey();

        int maxView = 0;
        for (Ad ad : sellerAds) {
            this.position += ad.getPosition();
            this.photo += Integer.parseInt(ad.getNumberPictures());
            this.textCount += Integer.parseInt(ad.getQuantityText());
            this.delivery += ad.getDelivery() ? 1 : 0;

            if (ad.getVip() || ad.getPremium() || ad.getXL() || ad.getUpped() || ad.getUrgent() ) {
                this.promAd++;
            }

            int dailyView = Integer.parseInt(ad.getDailyViews());
            if (ad.getData().contains(today) && maxView < dailyView) {
                maxView = dailyView;
                maxViewDate = ad.getData();
            }

            this.totalView += Integer.parseInt(ad.getViews());
            this.todayView += Integer.parseInt(ad.getDailyViews());
            this.yesterdayView += Integer.parseInt(ad.getViewYesterday());

            for (Map.Entry<String, Integer> day : ad.getRawStatData().entrySet()) {
                this.totalTenDaysView += day.getValue();
                this.maxTenDaysView = maxTenDaysView < day.getValue() ? day.getValue() : maxTenDaysView;
            }
        }

        this.photo = this.photo / adCount;
        this.position = this.position / adCount;
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
