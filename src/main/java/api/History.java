package api;

public class History {

    private String ip;
    private String nick;
    private String title;
    private int resultCount;
    private int time;
    private String url;

    public History() {}

    public History(String ip, String nick, String title, int resultCount, int time, String url) {
        this.ip = ip;
        this.nick = nick;
        this.title = title;
        this.resultCount = resultCount;
        this.time = time;
        this.url = url;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}