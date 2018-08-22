package parser;

public class Ad {

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
    }

    public Boolean hasStats() {
        return hasStats;
    }

    public void setHasStats(Boolean hasStats) {
        this.hasStats = hasStats;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Boolean getOnlyUpped() {
        return isOnlyUpped;
    }

    public void setOnlyUpped(Boolean onlyUpped) {
        isOnlyUpped = onlyUpped;
    }

    public Boolean getLessTwoVAS() {
        return isLessTwoVAS;
    }

    public void setLessTwoVAS(Boolean lessTwoVAS) {
        isLessTwoVAS = lessTwoVAS;
    }

    public Boolean getPremium() {
        return isPremium;
    }

    public void setPremium(Boolean premium) {
        isPremium = premium;
    }

    public Boolean getVip() {
        return isVip;
    }

    public void setVip(Boolean vip) {
        isVip = vip;
    }

    public Boolean getUrgent() {
        return isUrgent;
    }

    public void setUrgent(Boolean urgent) {
        isUrgent = urgent;
    }

    public Boolean getUpped() {
        return isUpped;
    }

    public void setUpped(Boolean upped) {
        isUpped = upped;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getViews() {
        return views;
    }

    public void setViews(String views) {
        this.views = views;
    }

    public String getDailyViews() {
        return dailyViews;
    }

    public void setDailyViews(String dailyViews) {
        this.dailyViews = dailyViews;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getNumberPictures() {
        return numberPictures;
    }

    public void setNumberPictures(String numberPictures) {
        this.numberPictures = numberPictures;
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

    public void setQuantityText(String quantityText) {
        this.quantityText = quantityText;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
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
    }

    public void addStatInfo(StatInfo info) {
        this.dateApplication = info.getDateApplication();
        this.viewsTenDay= info.getViewsTenDay();
        this.viewsAverageTenDay= info.getViewsAverageTenDay();
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
