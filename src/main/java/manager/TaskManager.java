package manager;

import socket.EventSocket;
import utility.ProxyManager;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskManager {

    private static CopyOnWriteArrayList<Task> tasks = new CopyOnWriteArrayList<>();

    static {
//        System.setErr(null);
//        doTask();
    }

    public static void initTask(String token, HashMap<String, String> parameters) {

        try {
            Task task = new Task(token, parameters);
            tasks.add(task);
        } catch (Exception e) {
            e.printStackTrace();
//            EventSocket.sendMessage(token, "{\"message\":\"error\",\"parameters\":[{\"name\":\"msg\",\"value\":\"Не удалось инициализировать таск\"}]}");
            return;
        }

//        EventSocket.sendMessage(token, "{\"message\":\"query\",\"parameters\":[{\"name\":\"position\",\"value\":\"" + tasks.size() + "\"}]}");
    }

    private static void updateQuery() {
        for (Task task : tasks) {
            EventSocket.sendMessage(task.getToken(),
                    "{\"message\":\"query\",\"parameters\":[{\"name\":\"position\",\"value\":\"" + (tasks.indexOf(task) + 1) + "\"}]}");
        }
    }

    public static void doTask() {
//        Thread demon = new Thread(() -> {
            while (true) {
                try {
                    if (tasks.size() > 0) {
                        Task task = tasks.get(0);
                        tasks.remove(task);

//                        boolean isExist = EventSocket.allTokens.containsKey(task.getToken());
//                        if (!isExist) continue;

                        task.start();

//                        EventSocket.sendResult(task);
//                        updateQuery();

                        System.gc();
                        ProxyManager.clear();
                    }
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                }
            }
//        });
//        demon.setDaemon(true);
//        demon.start();
    }
}
