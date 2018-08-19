package parser;

public class Ad {

    private String id;
    private String url;

    public Ad(PageInfo info) {
        this.id = info.getId();
        this.url = info.getUrl();
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

    }
}
