package api.resources;

import api.History;
import db.DBHandler;
import org.json.JSONArray;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
public class HistoryResource {

    @GET
    @Path("stats")
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
}
