package parser;

import java.io.Serializable;
import java.util.TreeMap;

public class Ad implements Serializable {

    private String id;
    private String url;
    private Integer position;

    private Boolean isOnlyUpped;
    private Boolean isLessTwoVAS;
    private Boolean isPremium;
    private Boolean isVip;
    private Boolean isUrgent;
    private Boolean isUpped;

    private String title;
    private String price;
    private String views;
    private String dailyViews;
    private String address;
    private String data;
    private String numberPictures;
    private String text;
    private String quantityText;
    private String seller;
    private String sellerId;
    private String phone;
    private Boolean hasStats;

    private String dateApplication;
    private String viewsTenDay;
    private String viewsAverageTenDay;
    private Integer maxTenDay;
    private String maxTenDate;

    private TreeMap<String, Integer> rawStatData = new TreeMap<>();

    // Новые элементы
    private Boolean isXL; // +
    private Boolean isPriceDown = false; // +
    private Boolean isDelivery = false; // +

    private Boolean isShop = false; // +
    private String activeAd = "1"; // +
    private String viewYesterday = "0"; // +

    public Ad(PageInfo info) {
        this.id = info.getId();
        this.url = info.getUrl();
        this.position = info.getPosition();
        this.isOnlyUpped = info.getOnlyUpped();
        this.isLessTwoVAS = info.getLessTwoVAS();
        this.isPremium = info.getPremium();
        this.isVip = info.getVip();
        this.isUrgent = info.getUrgent();
        this.isUpped = info.getUpped();

        this.isPriceDown = info.isPriceDown();
        this.isDelivery = info.isDelivery();
        this.isXL = info.isXL();
    }

    public void addPageInfo(ItemInfo info) {
        this.title = info.getTitle();
        this.price = info.getPrice();
        this.views = info.getViews();
        this.dailyViews = info.getDailyViews();
        this.address = info.getAddress();
        this.data = info.getData();
        this.numberPictures = info.getNumberPictures();
        this.text = info.getText();
        this.quantityText = info.getQuantityText();
        this.seller = info.getSeller();
        this.sellerId = info.getSellerId();
        this.phone= info.getPhone();
        this.hasStats= info.getHasStats();

        this.isShop = info.isShop();
        this.activeAd = info.getActiveAd();
    }

    public void addStatInfo(StatInfo info) {
        this.dateApplication = info.getDateApplication();
        this.viewsTenDay= info.getViewsTenDay();
        this.viewsAverageTenDay= info.getViewsAverageTenDay();
        this.maxTenDay = info.getMaxTenDay();
        this.maxTenDate = info.getMaxTenDate();
        this.viewYesterday = info.getViewYesterday();

        this.rawStatData = info.getRawData();
    }

    public Boolean getPriceDown() {
        return isPriceDown;
    }

    public Boolean getDelivery() {
        return isDelivery;
    }

    public Boolean getShop() {
        return isShop;
    }

    public String getViewYesterday() {
        return viewYesterday;
    }

    public TreeMap<String, Integer> getRawStatData() {
        return rawStatData;
    }

    public String getActiveAd() {
        return activeAd;
    }

    public Boolean getXL() {
        return isXL;
    }

    public Boolean getHasStats() {
        return hasStats;
    }

    public Integer getMaxTenDay() {
        return maxTenDay;
    }

    public String getMaxTenDate() {
        return maxTenDate;
    }

    public Boolean hasStats() {
        return hasStats;
    }

    public String getPhone() {
        return phone;
    }

    public String getDateApplication() {
        return dateApplication;
    }

    public String getViewsTenDay() {
        return viewsTenDay;
    }

    public String getViewsAverageTenDay() {
        return viewsAverageTenDay;
    }

    public Integer getPosition() {
        return position;
    }

    public Boolean getPremium() {
        return isPremium;
    }

    public Boolean getVip() {
        return isVip;
    }

    public Boolean getUrgent() {
        return isUrgent;
    }

    public Boolean getUpped() {
        return isUpped;
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }


    public String getViews() {
        return views;
    }

    public String getDailyViews() {
        return dailyViews;
    }

    public String getAddress() {
        return address;
    }

    public String getData() {
        return data;
    }

    public String getNumberPictures() {
        return numberPictures;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getQuantityText() {
        return quantityText;
    }

    public String getSeller() {
        return seller;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Ad{" +
                "id='" + id + '\'' +
//                ", url='" + url + '\'' +
                ", position=" + position +
                ", isOnlyUpped=" + isOnlyUpped +
                ", isLessTwoVAS=" + isLessTwoVAS +
                ", isPremium=" + isPremium +
                ", isVip=" + isVip +
                ", isUrgent=" + isUrgent +
                ", isUpped=" + isUpped +
                ", title='" + title + '\'' +
                ", price='" + price + '\'' +
                ", views='" + views + '\'' +
                ", dailyViews='" + dailyViews + '\'' +
                ", address='" + address + '\'' +
                ", data='" + data + '\'' +
                ", numberPictures='" + numberPictures + '\'' +
//                ", text='" + text + '\'' +
                ", quantityText='" + quantityText + '\'' +
                ", seller='" + seller + '\'' +
                ", dateApplication='" + dateApplication + '\'' +
                ", viewsTenDay='" + viewsTenDay + '\'' +
                ", viewsAverageTenDay='" + viewsAverageTenDay + '\'' +
                '}';
    }
}
