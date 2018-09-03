package db;

import api.History;
import com.mysql.cj.jdbc.MysqlDataSource;
import manager.RequestTask;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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


}
