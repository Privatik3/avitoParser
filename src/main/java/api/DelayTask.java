package api;

import java.util.Date;
import java.util.HashMap;

public class DelayTask {

    private String taksID;

    private String nick;

    private String title;
    private String url;

    private Date date;
    private Status status;
    private HashMap<String, String> params;

    public static Status getStatus(String status) {
        switch (status) {
            case "QUEUE": return Status.QUEUE;
            case "PROCESSING": return Status.PROCESSING;
            case "COMPLETE": return Status.COMPLETE;
            case "FAIL": return Status.FAIL;
            case "ZERO_RESULT": return Status.ZERO_RESULT;
        }

        return Status.UNDEFINE;
    }

    public enum Status {
        QUEUE,
        PROCESSING,
        COMPLETE,
        FAIL,
        ZERO_RESULT,
        UNDEFINE
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getTaksID() {
        return taksID;
    }

    public void setTaksID(String taksID) {
        this.taksID = taksID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public void setParams(HashMap<String, String> params) {
        this.params = params;
    }
}
