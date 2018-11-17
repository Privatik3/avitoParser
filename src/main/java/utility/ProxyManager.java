package utility;

import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class ProxyManager {

    private static Logger log = Logger.getLogger("");
    private static ArrayList<RequestConfig> PROXIES = new ArrayList<>();
    private static long lastUpdate = 0;

    private static ArrayList<RequestConfig> loadProxiesFormApi() {
        ArrayList<RequestConfig> tmpProxy = new ArrayList<>();

        try {
            ArrayList<String> proxies = new ArrayList<>();
            for (String api : Files.readAllLines(Paths.get("proxy-api.txt"))) {
                try {
                    List<String> body = Arrays.asList(Jsoup.connect(api)
                            .timeout(10 * 1000)
                            .ignoreContentType(true)
                            .userAgent("Mozilla/5.0 (Windows NT 6.1; rv:40.0) Gecko/20100101 Firefox/40.0")
                            .method(Connection.Method.GET)
                            .get()
                            .body()
                            .text().split("\\s"));
                    if (body.size() > 250)
                        proxies.addAll(body);
                } catch (Exception ignored) {}
            }

            if (proxies.size() < 250) {
                proxies.clear();
                proxies.addAll(Files.readAllLines(Paths.get("proxy-list.txt")));
                log.info("-------------------------------------------------");
                log.info("Прокси были востановленны");

            } else {
//                Files.write(Paths.get("proxy-list.txt"), proxies);
            }

            for (String proxy : proxies) {
                try {
                    String hostName = proxy.split(":")[0];
                    int port = Integer.parseInt(proxy.split(":")[1]);

                    boolean present = tmpProxy.stream().anyMatch(p -> p.getProxy().getHostName().equals(hostName));
                    if (!present) {
                        tmpProxy.add(RequestConfig.custom()
                                .setCookieSpec(CookieSpecs.STANDARD_STRICT)
                                .setProxy(new HttpHost(hostName, port, "http"))
                                .setConnectionRequestTimeout(10 * 1000)
                                .setSocketTimeout(10 * 1000)
                                .setConnectTimeout(10 * 1000).build());
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            return tmpProxy;
        }

        return tmpProxy;
    }

    private static void loadProxies() {
        ArrayList<RequestConfig> tmpProxy = loadProxiesFormApi();
        if (tmpProxy.size() != 0) {
            lastUpdate = new Date().getTime();
            PROXIES = tmpProxy;
        }
    }

    public static ArrayList<RequestConfig> getProxy() throws Exception {
        if (PROXIES.size() < 250 || new Date().getTime() - lastUpdate > 1 * 60 * 60 * 1000)
            loadProxies();

        if (PROXIES.size() == 0) throw new Exception("Не удалось получить прокси");
        return PROXIES;
    }

    public static void sort(ArrayList<RequestConfig> allProxy) {
        PROXIES.clear();
        PROXIES.addAll(allProxy);
    }
}