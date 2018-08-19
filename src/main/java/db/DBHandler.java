package db;

import api.History;
import manager.RequestTask;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBHandler {

    private static Whitelist whitelist = new Whitelist();
    private static Logger log = Logger.getLogger(DBHandler.class.getName());
    private static Connection conn;

    static {
        try {
            conn = DriverManager.getConnection("jdbc:h2:C:/Developers/avitoParser/cache");
            conn.setAutoCommit(true);

            String[] tags = new String[] {"a", "abbr", "address", "area", "article", "aside", "audio", "b", "base", "bdi", "bdo", "blockquote", "body", "br", "button", "canvas", "caption", "cite", "code", "col", "colgroup", "data", "datalist", "dd", "del", "details", "dfn", "dialog", "div", "dl", "dt", "em", "embed", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hgroup", "hr", "html", "i", "iframe", "img", "input", "ins", "kbd", "keygen", "label", "legend", "li", "main", "map", "mark", "math", "menu", "menuitem", "meter", "nav", "noscript", "object", "ol", "optgroup", "option", "output", "p", "param", "picture", "pre", "progress", "q", "rb", "rp", "rt", "rtc", "ruby", "s", "samp", "section", "select", "slot", "small", "source", "span", "strong", "sub", "summary", "sup", "svg", "table", "tbody", "td", "template", "textarea", "tfoot", "th", "thead", "time", "title", "tr", "track", "u", "ul", "var", "video", "wbr"};
            String[] attrs = new String[] {"id", "class", "data-asin", "alt", "href", "content"};

            whitelist.addTags(tags);
            for (String tag : tags)
                whitelist.addAttributes(tag, attrs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void addAvitoItems(ArrayList<RequestTask> items) {
        addToTable(items, "AVITO_ITEMS");
    }

    public static void addAvitoPages(ArrayList<RequestTask> items) {
        addToTable(items, "AVITO_PAGES");
    }

    private static void addToTable(ArrayList<RequestTask> items, String tabName) {

        if (items.size() == 0) return;

        PreparedStatement insertStatement = null;
        try {
            String sql = "INSERT INTO " + tabName + " (ASIN, HTML) values (?, ?)";
            insertStatement = conn.prepareStatement(sql);

            for (RequestTask item : items) {
                insertStatement.setString(1, item.getId());
                insertStatement.setString(2, Jsoup.clean(item.getHtml(), whitelist));
                insertStatement.addBatch();
            }

            insertStatement.executeBatch();
        } catch (Exception e) {
            log.info("-------------------------------------------------");
            log.log(Level.SEVERE, "Не удалось занести Amazon Items в базу");
            log.log(Level.SEVERE, "Exception: " + e.getMessage());
        } finally {
            items.clear();
            items = null;

            try {
                if (insertStatement != null) {
                    insertStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<RequestTask> selectAllItems() {
        return select("AVITO_ITEMS");
    }

    public static List<RequestTask> selectAllPages() {
        return select("AVITO_PAGES");
    }

    private static List<RequestTask> select(String tabName) {
        Statement statement = null;
        List<RequestTask> result = new ArrayList<>();
        try {
            statement = conn.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = statement.executeQuery("SELECT * FROM " + tabName);

            while (rs.next()) {
                String asin = rs.getString(2);
                String html = rs.getString(3);

                result.add(new RequestTask(asin, html));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static void clearAvitoItems() {
        clearTable("AVITO_ITEMS");
    }

    public static void clearAvitoPages() {
        clearTable("AVITO_PAGES");
    }

    private static void clearTable(String tabName) {
        Statement statement = null;
        try {
            statement = conn.createStatement();
            statement.executeUpdate("DELETE FROM " + tabName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<History> getHistoryByNick(String nick) {
        return null;
    }


}
