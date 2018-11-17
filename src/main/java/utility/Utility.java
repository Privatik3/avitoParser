package utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Utility {

    public static HashMap<String, ArrayList<String>> parseTaskParams(String json) {

        JSONObject obj = new JSONObject(json);
        JSONArray params = obj.getJSONArray("parameters");

        System.out.println("Получена команда к старту, ПАРАМЕТРЫ:");
        HashMap<String, ArrayList<String>> param = new HashMap<>();
        for (int i = 0; i < params.length(); i++) {
            String name = params.getJSONObject(i).getString("name");
            String value = params.getJSONObject(i).getString("value");

            if (!value.isEmpty())
                param.computeIfAbsent(name, k -> new ArrayList<>()).add(value);

            System.out.println("    *" + name + ": " + value);
        }

        return param;
    }
}
