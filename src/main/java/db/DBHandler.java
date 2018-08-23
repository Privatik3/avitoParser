package db;

import api.History;
import manager.RequestTask;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBHandler {

    private static JDBCTemplate jdbcTemplate;

    static {
        try {
            ApplicationContext context = new ClassPathXmlApplicationContext("Beans.xml");
            jdbcTemplate = (JDBCTemplate) context.getBean("adJDBCTemplate");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveHistory(History record) {
        jdbcTemplate.createHistoryRecord(record);
    }

    public static List<History> getHistoryByNick(String nick) {
        return jdbcTemplate.getHistory(nick);
    }


}
