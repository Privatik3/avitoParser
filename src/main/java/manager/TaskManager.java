package manager;

import socket.EventSocket;
import utility.ProxyManager;

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
        System.setErr(null);
        system.info("-------------------------------------------------");
        doTask();
    }

    public static void initTask(String token, HashMap<String, ArrayList<String>> params) {

        try {
            Task task = new Task(token, params);
            tasks.add(task);
        } catch (Exception e) {
            EventSocket.sendMessage(token, "{\"message\":\"error\",\"parameters\":[{\"name\":\"msg\",\"value\":\"" + e.getMessage() + "\"}]}");
            EventSocket.closeToken(token);
            return;
        }

        EventSocket.sendMessage(token, "{\"message\":\"query\",\"parameters\":[{\"name\":\"position\",\"value\":\"" + tasks.size() + "\"}]}");
    }

    private static void updateQuery() {
        for (Task task : tasks) {
            EventSocket.sendMessage(task.getToken(),
                    "{\"message\":\"query\",\"parameters\":[{\"name\":\"position\",\"value\":\"" + (tasks.indexOf(task) + 1) + "\"}]}");
        }
    }

    public static void doTask() {
        Thread demon = new Thread(() -> {
            while (true) {
                String token = "";
                try {
                    if (tasks.size() > 0) {
                        Task task = tasks.get(0);
                        tasks.remove(task);

                        token = task.getToken();
                        EventSocket.checkToken(token);

                        task.start();
                        updateQuery();

                        System.gc();
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    EventSocket.sendMessage(token, "{\"message\":\"error\",\"parameters\":[{\"name\":\"msg\",\"value\":\"" + e.getMessage() + "\"}]}");
                    EventSocket.closeToken(token);
                }
            }
        });
        demon.setDaemon(true);
        demon.start();
    }
}
