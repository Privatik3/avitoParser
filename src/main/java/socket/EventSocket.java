package socket;

import manager.Task;
import manager.TaskManager;
import org.json.JSONArray;
import org.json.JSONObject;
import utility.Utility;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ClientEndpoint
@ServerEndpoint(value = "/task")
public class EventSocket {

    public static HashMap<String, Session> allTokens = new HashMap<>();
    private String token;

    public static void sendResult(Task task) throws IOException {
        String token = task.getToken();
        EventSocket.sendMessage(token,
                "{\"message\":\"done\",\"parameters\":[{\"name\":\"url\",\"value\":\"" + task.getResultLink() +"\"}]}");

        getSess(token).close();
    }

    public static void closeToken(String token) {
        try {
            getSess(token).close();
        } catch (Exception ignored) {}
    }

    @OnOpen
    public void onWebSocketConnect(Session sess) throws IOException {
        String token = getToken(sess);
        if (allTokens.containsKey(token)) {
            String message = "{\"message\":\"error\",\"parameters\":[{\"name\":\"msg\",\"value\":\"Не удалось открыть соединения, возможно вы уже запустили парсинг.\"}]}";
            sess.getBasicRemote().sendText(message);
            sess.close();
        } else {
            System.out.println("ТОКЕН ДОБАВЛЕН В БАЗУ: " + token);
            allTokens.put(token, sess);
            this.token = token;
        }
    }

    public static void sendMessage(String token, String message) {
        try {
            getSess(token).getBasicRemote().sendText(message);
        } catch (Exception e) {
            System.err.println("Не могу послать собщение по веб-сокету, токен: " + token);
        }
    }

    private static Session getSess(String token) {
        return allTokens.get(token);
    }

    private String getToken(Session sess) {
        Map<String, List<String>> requestParam = sess.getRequestParameterMap();
        return requestParam.get("token").get(0);
    }

    @OnMessage
    public void onWebSocketText(String json) throws IOException, InterruptedException {

        JSONObject obj = new JSONObject(json);
        String message = obj.getString("message");

        switch (message) {
            case "start": {
                HashMap<String, ArrayList<String>> param = Utility.parseTaskParams(json);
                TaskManager.initTask(token, param);
            }
        }
    }

    @OnClose
    public void onWebSocketClose(CloseReason reason) {
        if (token != null) {
            System.out.println("ЗАКРЫВАЮ СОЕДЕНЕНИЕ, TOKEN: " + token);
            allTokens.remove(token);
        } else {
            System.out.println("ТАКОЙ ТОКЕН УЖЕ ОТКРЫТ!!!");
        }
    }

    @OnError
    public void onWebSocketError(Throwable cause) {
        cause.printStackTrace(System.err);
    }

    public static void checkToken(String token) throws Exception {
        if (!allTokens.containsKey(token))
            throw new Exception("Пользователь прервал соединения");
    }
}