package manager;

import socket.EventSocket;
import utility.ProxyManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;
import java.util.logging.Formatter;

public class TaskManager {

    private static CopyOnWriteArrayList<Task> tasks = new CopyOnWriteArrayList<>();

    static {
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
//        System.setErr(null);
        system.info("-------------------------------------------------");
        doTask();
    }

    public static void initTask(String token, HashMap<String, String> parameters) throws IOException {

        Task task = new Task(token, parameters);
        tasks.add(task);

        EventSocket.sendMessage(token, "{\"message\":\"query\",\"parameters\":[{\"name\":\"position\",\"value\":\"" + tasks.size() + "\"}]}");
    }

    private static void updateQuery() {
        for (Task task : tasks) {
            EventSocket.sendMessage(task.getToken(),
                    "{\"message\":\"query\",\"parameters\":[{\"name\":\"position\",\"value\":\"" + (tasks.indexOf(task) + 1) + "\"}]}");
        }
    }

    private static void doTask() {
        Thread demon = new Thread(() -> {
            while (true) {
                try {
                    if (tasks.size() > 0) {
                        Task task = tasks.get(0);
                        tasks.remove(task);

                        boolean isExist = EventSocket.allTokens.containsKey(task.getToken());
                        if (!isExist) continue;

                        task.start();

                        EventSocket.sendResult(task);
                        updateQuery();

                        System.gc();
                        ProxyManager.clear();
                    }
                    Thread.sleep(500);
                } catch (Exception ignored) {
                }
            }
        });
        demon.setDaemon(true);
        demon.start();
    }
}
