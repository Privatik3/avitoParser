package db;

import api.History;
import api.HistoryStats;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
}
