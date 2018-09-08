package api.resources;

import api.History;
import api.HistoryStats;
import db.DBHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.List;

@Path("/")
public class HistoryResource {

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
        } catch (Exception ignore) { ignore.printStackTrace();}

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
        } catch (Exception ignore) { ignore.printStackTrace();}

        return "{}";
    }

    @GET
    @Path("report.xlsx")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public File getFile(@QueryParam("fileID") String fileID) {
        return new File("reports/" + fileID + ".xlsx");
    }
}
