import manager.TaskManager;
import org.json.JSONObject;
import utility.ProxyManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.*;

public class MainTest {

    public static void main(String[] args) {

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

        String pageCount = "30"; // Количество страниц

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("category_id", "14");
        parameters.put("location_id", "653240");
        parameters.put("s", "101");
        parameters.put("s_trg", "3");
        parameters.put("max_pages", pageCount);

        TaskManager.initTask("646421", parameters);

        TaskManager.doTask();
    }
}
