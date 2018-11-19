package parser;

public class PageInfo {

    private String id;
    private String url;
    private Integer position;

    private Boolean isOnlyUpped;
    private Boolean isLessTwoVAS;
    private Boolean isPremium;
    private Boolean isVip;
    private Boolean isUrgent;
    private Boolean isUpped;

    private Boolean isXL = false;
    private Boolean isPriceDown = false;
    private Boolean isDelivery = false;

    public Boolean isPriceDown() {
        return isPriceDown;
    }

    public void setPriceDown(Boolean priceDown) {
        isPriceDown = priceDown;
    }

    public Boolean isDelivery() {
        return isDelivery;
    }

    public void setDelivery(Boolean delivery) {
        isDelivery = delivery;
    }

    public boolean isXL() {
        return isXL;
    }

    public void setXL(boolean XL) {
        isXL = XL;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
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
}
