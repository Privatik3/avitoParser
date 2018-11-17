package api.resources;

import api.DelayTask;
import api.History;
import api.HistoryStats;
import db.DBHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import sun.misc.Contended;
import utility.Utility;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@Path("/")
public class HistoryResource {

    @POST
    @Path("add_delay_task")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addDelayTask(String json) {
        System.out.println(json);

        try {
            HashMap<String, ArrayList<String>> params = Utility.parseTaskParams(json);
            String token = params.get("token").get(0);
            params.remove("token");

            DBHandler.createDelayTask(token, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GET
    @Path("history")
    @Produces(MediaType.APPLICATION_JSON)
    public String hello(@QueryParam("page") String page,
                        @QueryParam("pageSize") String pageSize,
                        @QueryParam("nick") String nick,
                        @QueryParam("orderBy") String orderBy) {

        try {
            int numPage = Integer.parseInt(page);
            int numPageSize = Integer.parseInt(pageSize);

            List<History> result = DBHandler.getHistory(numPage, numPageSize, nick, orderBy);
            JSONArray jsonObject = new JSONArray(result.toArray());
            return jsonObject.toString();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        return "{}";
    }

    @GET
    @Path("remove")
    @Produces(MediaType.APPLICATION_JSON)
    public String removeTask(@QueryParam("taskid") String taskID) {
        try {
            DBHandler.removeDelayTask(taskID);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        return "{}";
    }

    @GET
    @Path("delay_task")
    @Produces(MediaType.APPLICATION_JSON)
    public String delayTask(@QueryParam("page") String page,
                            @QueryParam("pageSize") String pageSize,
                            @QueryParam("nick") String nick,
                            @QueryParam("orderBy") String orderBy) {

        try {
            int numPage = Integer.parseInt(page);
            int numPageSize = Integer.parseInt(pageSize);

            List<DelayTask> delayTasks = DBHandler.getDelayTasks(numPage, numPageSize, nick, orderBy);
            JSONArray jsonObject = new JSONArray(delayTasks.toArray());
            return jsonObject.toString();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        return "{}";
    }

    @GET
    @Path("delay_task_count")
    @Produces(MediaType.APPLICATION_JSON)
    public String delayTaskCount(@QueryParam("nick") String nick) {

        try {
            Integer count = DBHandler.getDelayTasksCount(nick);

            HashMap<String, Integer> result = new HashMap<>();
            result.put("count", count);

            return new JSONObject(result).toString();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        return "{}";
    }

    @GET
    @Path("stats")
    @Produces(MediaType.APPLICATION_JSON)
    public String hello(@QueryParam("nick") String nick) {

        try {
            HistoryStats stats = DBHandler.getHistoryStats(nick);
            JSONObject jsonObject = new JSONObject(stats);
            return jsonObject.toString();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        return "{}";
    }

    @GET
    @Path("filters/list")
    @Produces(MediaType.APPLICATION_JSON)
    public String filters(@Context UriInfo uriInfo, @QueryParam("category_id") String categoryID) throws InterruptedException, IOException {
        String filterUrl =
                "https://www.avito.ru/search/filters/list?_=6&" +
                        uriInfo.getRequestUri().getQuery() +
                        "&currentPage=catalog&filtersGroup=catalog";

        return Jsoup.connect(filterUrl).
                ignoreContentType(true).execute().body();
    }

    @GET
    @Path("filters/locations")
    @Produces(MediaType.APPLICATION_JSON)
    public String locations(@QueryParam("region") String region) {

        String locationUrl =
                "https://www.avito.ru/js/locations?json=true&id=" + region + "&_=f864176";

        try {
            return Jsoup.connect(locationUrl).
                    ignoreContentType(true).execute().body();
        } catch (Exception e) {
            return "{}";
        }
    }

    @GET
    @Path("filters/directions")
    @Produces(MediaType.APPLICATION_JSON)
    public String directions(@QueryParam("city") String city) {

        String locationUrl =
                "https://www.avito.ru/js/directions?json=true&catid=&filter=1&locid=" + city + "&_=f864176";

        try {
            return Jsoup.connect(locationUrl).
                    ignoreContentType(true).execute().body();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    @GET
    @Path("report/{fileName}")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public File getFile(@QueryParam("fileID") String fileID) {
        return new File("reports/" + fileID + ".xlsx");
    }
}
