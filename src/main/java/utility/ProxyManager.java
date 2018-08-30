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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class ProxyManager {

    private static ArrayList<RequestConfig> PROXIES = new ArrayList<>();
    private static long lastUpdate = 0;

    private static void loadProxies() {

        ArrayList<RequestConfig> tmpProxy = new ArrayList<>();
        try {
            List<String> proxyApi = Files.readAllLines(Paths.get("proxy-list.txt"));

            for (String api : proxyApi) {
                String[] proxies = Jsoup.connect(api)
                        .timeout(10 * 1000)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; rv:40.0) Gecko/20100101 Firefox/40.0")
                        .method(Connection.Method.GET)
                        .get()
                        .body()
                        .text().split("\\s");

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
            }
            PROXIES.clear();
            PROXIES.addAll(tmpProxy);
            tmpProxy.clear();

            lastUpdate = new Date().getTime();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<RequestConfig> getProxy() {

        if (PROXIES.size() == 0 || new Date().getTime() - lastUpdate > 60 * 60 * 1000)
            loadProxies();

        return PROXIES;
    }

    public static void sort(ArrayList<RequestConfig> allProxy) {

        PROXIES.clear();
        PROXIES.addAll(allProxy);
    }
}
