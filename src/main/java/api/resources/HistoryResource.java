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
    public String hello(@QueryParam("nick") String nick) {

        List<History> result = DBHandler.getHistoryByNick(nick);

        JSONArray jsonObject = new JSONArray(result.toArray());
        return jsonObject.toString();
    }
}
