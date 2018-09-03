package api;

import java.util.Date;

public class History {

    private String ip;
    private String nick;
    private String title;
    private int resultCount;
    private int time;
    private String url;
    private Date date;
    private RecordType type;

    public History() {}

    public History(String ip, String nick, int resultCount, int time, String title, String url, Date date, RecordType type) {
        this.ip = ip;
        this.nick = nick;
        this.resultCount = resultCount;
        this.time = time;
        this.title = title;
        this.url = url;
        this.date = date;
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public String getNick() {
        return nick;
    }

    public String getTitle() {
        return title;
    }

    public int getResultCount() {
        return resultCount;
    }

    public int getTime() {
        return time;
    }

    public String getUrl() {
        return url;
    }

    public Date getDate() {
        return date;
    }

    public RecordType getType() {
        return type;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setType(RecordType type) {
        this.type = type;
    }
}