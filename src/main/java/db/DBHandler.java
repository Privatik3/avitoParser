package db;

import api.DelayTask;
import api.History;
import api.HistoryStats;
import com.mysql.cj.jdbc.MysqlDataSource;
import manager.Task;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class DBHandler {

    private static JDBCTemplate jdbcTemplate = null;

    static {
        Properties props = new Properties();

        try(FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
            MysqlDataSource mysqlDS = new MysqlDataSource();
            mysqlDS.setURL(props.getProperty("MYSQL_DB_URL"));
            mysqlDS.setUser(props.getProperty("MYSQL_DB_USERNAME"));
            mysqlDS.setPassword(props.getProperty("MYSQL_DB_PASSWORD"));

            jdbcTemplate = new JDBCTemplate();
            jdbcTemplate.setDataSource(mysqlDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveHistory(History record) {
        if (jdbcTemplate != null)
            jdbcTemplate.createHistoryRecord(record);
    }

    public static List<History> getHistory(int page, int pageSize, String nick, String orderBy) {
        if (jdbcTemplate != null)
            return jdbcTemplate.getHistory(page, pageSize, nick, orderBy);
        else
            return new ArrayList<>();
    }

    public static HistoryStats getHistoryStats(String nick) {
        if (jdbcTemplate != null)
            return jdbcTemplate.getHistoryStats(nick);
        else
            return new HistoryStats();
    }

    public static List<DelayTask> getDelayTasks(int page, int pageSize, String nick, String orderBy) {
        if (jdbcTemplate != null)
            return jdbcTemplate.getDelayTasks(page, pageSize, nick, orderBy);
        else
            return new ArrayList<>();
    }

    public static Integer getDelayTasksCount(String nick) {
        if (jdbcTemplate != null)
            return jdbcTemplate.getDelayTasksCount(nick);
        else
            return 0;
    }

    public static boolean checkDelayTask() {
        if (jdbcTemplate != null)
            return jdbcTemplate.checkDelayTask();
        else
            return false;
    }


    public static Task getFreeTask() {
        if (jdbcTemplate != null)
            return jdbcTemplate.getFreeTask();
        else
            return null;
    }

    public static void changeDelayTaskStatus(String id, DelayTask.Status status) {
        if (jdbcTemplate != null)
            jdbcTemplate.changeDelayTaskStatus(id, status);
    }

    public static void createDelayTask(String token, HashMap<String, ArrayList<String>> params) throws ParseException {
        if (jdbcTemplate != null)
            jdbcTemplate.createDelayTask(token, params);
    }

    public static void removeDelayTaskParams(String taskID) {
        if (jdbcTemplate != null)
            jdbcTemplate.removeDelayTaskParams(taskID);
    }

    public static void updateDelayTaskReport(String taskID, String resultLink) {
        if (jdbcTemplate != null)
            jdbcTemplate.updateDelayTaskReport(taskID, resultLink);
    }

    public static void removeDelayTask(String taskID) {
        if (jdbcTemplate != null)
            jdbcTemplate.removeDelayTask(taskID);
    }
}
