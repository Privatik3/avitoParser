package parser;

public class ItemInfo {

    private String id;

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

    private Boolean isShop = false;
    private String activeAd = "1";

    public Boolean isShop() {
        return isShop;
    }

    public void setShop(Boolean shop) {
        isShop = shop;
    }

    public String getActiveAd() {
        return activeAd;
    }

    public void setActiveAd(String activeAd) {
        this.activeAd = activeAd;
    }

    public Boolean getHasStats() {
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

    public String getQuantityText() {
        return quantityText;
    }

    public void setQuantityText(String quantityText) {
        this.quantityText = quantityText;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getNumberPictures() {
        return numberPictures;
    }

    public void setNumberPictures(String numberPictures) {
        this.numberPictures = numberPictures;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String adress) {
        this.address = adress;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
