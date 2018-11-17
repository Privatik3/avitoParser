package socket;

import javax.servlet.DispatcherType;
import javax.websocket.server.ServerContainer;

import manager.TaskManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.EnumSet;

import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

public class EventServer {

    public static void main(String[] args) throws InterruptedException {

        new Thread(EventServer::startApiServer).start();
        startSocketServer();
    }

    private static void startSocketServer() {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        try {
            // Initialize javax.websocket layer
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            // Add WebSocket endpoint to javax.websocket layer
            wscontainer.addEndpoint(EventSocket.class);

            server.start();
//            server.dump(System.err);
            System.err.println("-----------------------------------------------");
            server.join();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    private static void startApiServer() {
        Server server = new Server(8081);

        ServletContextHandler context = new ServletContextHandler(NO_SESSIONS);

        context.setContextPath("/");
        FilterHolder cors = context.addFilter(CrossOriginFilter.class,"/*",EnumSet.of(DispatcherType.REQUEST));
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");

        server.setHandler(context);

        ServletHolder servletHolder = context.addServlet(ServletContainer.class, "/api/*");
        servletHolder.setInitOrder(0);
        servletHolder.setInitParameter(
                "jersey.config.server.provider.packages",
                "api.resources"
        );

        try {
            server.start();
            server.join();
        } catch (Exception ex) {
            System.err.println("Error occurred while starting API Server:\n" + ex);
            System.exit(1);
        }

        finally {
            server.destroy();
        }
    }
}