package api;

public class HistoryStats {

    private int allCount;

    private int avgOneCount;
    private int avgOneTime;

    private int avgTenCount;
    private int avgTenTime;

    public HistoryStats() { }

    public int getAllCount() {
        return allCount;
    }

    public void setAllCount(int allCount) {
        this.allCount = allCount;
    }

    public int getAvgOneCount() {
        return avgOneCount;
    }

    public void setAvgOneCount(int avgOneCount) {
        this.avgOneCount = avgOneCount;
    }

    public int getAvgOneTime() {
        return avgOneTime;
    }

    public void setAvgOneTime(int avgOneTime) {
        this.avgOneTime = avgOneTime;
    }

    public int getAvgTenCount() {
        return avgTenCount;
    }

    public void setAvgTenCount(int avgTenCount) {
        this.avgTenCount = avgTenCount;
    }

    public int getAvgTenTime() {
        return avgTenTime;
    }

    public void setAvgTenTime(int avgTenTime) {
        this.avgTenTime = avgTenTime;
    }
}
