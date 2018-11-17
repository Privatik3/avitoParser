import api.DelayTask;
import api.History;
import api.HistoryStats;
import api.RecordType;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import db.DBHandler;
import manager.TaskManager;
import org.json.JSONObject;
import utility.ProxyManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public class MainTest {

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {


        String token1 = "Test";

        HashMap<String, ArrayList<String>> params = new HashMap<>();
        params.put("category_id", new ArrayList<>(Collections.singleton("40")));
        params.put("location_id", new ArrayList<>(Collections.singleton("621540")));
        params.put("params[181]", new ArrayList<>(Collections.singleton("795")));
        params.put("s", new ArrayList<>(Collections.singleton("101")));
        params.put("max_pages", new ArrayList<>(Collections.singleton("1")));
        params.put("description", new ArrayList<>(Collections.singleton("true")));
        params.put("ip", new ArrayList<>(Collections.singleton("188.243.70.188")));
        params.put("title", new ArrayList<>(Collections.singleton("Авито | чехлы | Транспорт | Запчасти и аксессуары | По всей России | 2018-09-26 08:15:09")));

        DBHandler.createDelayTask(token1, params);

        System.exit(1);

        List<String> allLines = Files.readAllLines(Paths.get("history.csv"));
        for (String line : allLines) {
            String[] data = line.split(";");

            String ip = data[1].replace("\"", "");
            String token = data[2].replace("\"", "");
            String title = data[3].replace("\"", "");
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(title.substring(title.lastIndexOf("|") + 2));
            String resultLink = data[4].replace("\"", "");
            if (resultLink.isEmpty()) continue;

            int size = Integer.parseInt(data[5].replace("\"", ""));
            if(size == 0) continue;

            int endTime = Integer.parseInt(data[6].replace("\"", ""));
            RecordType type = RecordType.GOOGLE_DOCS;

            History record = new History(
                    ip, token, size, endTime, title.replaceAll("\\s\\|\\s\\d+-.*$", ""),
                    resultLink, date, type);
            DBHandler.saveHistory(record);
        }

        System.exit(1);

        Logger system = Logger.getLogger("");
        Handler[] handlers = system.getHandlers();
        system.removeHandler(handlers[0]);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return
                        new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " -> " +
                                record.getMessage() + "\r\n";
            }
        });
        system.addHandler(handler);
        system.setUseParentHandlers(false);
        system.info("-------------------------------------------------");

        String pageCount = "1"; // Количество страниц

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("category_id", "14");
        parameters.put("location_id", "653240");
        parameters.put("s", "101");
        parameters.put("s_trg", "3");

        parameters.put("title", "Авито - Транспорт - Мотоциклы и мототехника - 2018-08-22");
        parameters.put("ip", "176.105.204.178");
        parameters.put("max_pages", pageCount);

        parameters.put("photo", "true");
        parameters.put("description", "true");
        parameters.put("descriptionLength", "true");
        parameters.put("sellerName", "true");
//        parameters.put("position", "true");
//        parameters.put("date", "true");
//        parameters.put("phone", "true");

//        TaskManager.initTask("xiiiangel", parameters);

//        TaskManager.doTask();
    }
}
