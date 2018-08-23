package utility;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import co.paralleluniverse.strands.SuspendableRunnable;
import db.DBHandler;
import manager.ReqTaskType;
import manager.RequestTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestManager {

    private static Logger log = Logger.getLogger(RequestManager.class.getName());
    private static CloseableHttpClient client = null;
    private static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0";

    private static void initClient() {
        try {
            TrustStrategy acceptingTrustStrategy = (certificate, authType) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            int cores = Runtime.getRuntime().availableProcessors();

            client = FiberHttpClientBuilder.
                    create(cores * 2).
                    setUserAgent(USER_AGENT).
                    setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).
                    setSSLContext(sslContext).
                    setMaxConnPerRoute(1024).
                    setMaxConnTotal(1024).build();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Не удалось инициализировать HTTP Client");
            log.log(Level.SEVERE, "Exception: ", e);
        }
    }

    public static List<RequestTask> execute(ArrayList<RequestTask> tasks) throws Exception {

        if (client == null)
            initClient();

        HashSet<RequestTask> result = new HashSet<>();

        ArrayList<RequestConfig> allProxy = ProxyManager.getProxy();
        ArrayList<RequestConfig> goodProxy = new ArrayList<>();

        final long startTime = new Date().getTime();
        ArrayList<RequestTask> taskMultiply = new ArrayList<>(tasks);

        Integer waveCount = 0;
        Integer parseSpeed = 0;
        Integer wave = 0;
        Integer resultStatus = 0;
        Integer failCount = 0;

        ArrayList<RequestConfig> proxys;
        while (tasks.size() > 0) {

            if (resultStatus == result.size())
                failCount = failCount + 1;
            else
                resultStatus = result.size();

            log.info("-------------------------------------------------");
            log.info("Инициализируем новую волну, осталось: " + taskMultiply.size() + " тасков");

            if (wave != 0) {
                parseSpeed = ((parseSpeed * waveCount) + (wave - taskMultiply.size())) / ++waveCount;
                log.info("Среднее количество страниц на волну: " + parseSpeed);
            }
            wave = taskMultiply.size();

            tasks.clear();
            for (int i = 0; tasks.size() < (allProxy.size() > 1024 ? 1024 : allProxy.size())
                    && tasks.size() < (taskMultiply.size() * (taskMultiply.size() == 1 ? 1 : 4)); i++) {
                if (i == taskMultiply.size())
                    i = 0;

                tasks.add(taskMultiply.get(i));
            }

            final CountDownLatch cdl = new CountDownLatch(tasks.size());

            proxys = new ArrayList<>(goodProxy);
            goodProxy.clear();
            if (goodProxy.size() < tasks.size())
                proxys.addAll(allProxy);

            for (int i = 0; i < proxys.size() && i < tasks.size(); i++) {
                RequestTask task = tasks.get(i);
                RequestConfig proxy = proxys.get(i);

                new Fiber<Void>((SuspendableRunnable) () -> {
                    HttpEntity entity = null;
                    try {
                        String taskUrl = task.getUrl().replaceAll("https", "http");
                        HttpGet request = new HttpGet(taskUrl);
                        if (tasks.size() != 1)
                            request.setConfig(proxy);

                        CloseableHttpResponse response = client.execute(request);
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            entity = response.getEntity();
                            String body = EntityUtils.toString(entity, "UTF-8");

                            if (body.contains("499bdc75d3636c55") || body.contains("601e767b7f3255ac") || body.contains("data-chart")) {
                                task.setHtml(body);
                                result.add(task);
                                goodProxy.add(proxy);
                            }
                        }
                    } catch (IOException e) {
//                        log.log(Level.SEVERE, "Ошибка внутри вайба");
//                        log.log(Level.SEVERE, "Exception: ", e);
                    } finally {
                        if (entity != null) {
                            try {
                                EntityUtils.consume(entity);
                            } catch (IOException ex) {
                                log.log(Level.SEVERE, "Не удалось освободить Entity, ресурсы заблокированы");
                                log.log(Level.SEVERE, "Exception: ", ex);
                            }
                        }
                    }
                }).start();

            }

            cdl.await(10, TimeUnit.SECONDS);

            taskMultiply.removeAll(result);
            if (resultStatus == result.size() && taskMultiply.size() != 0 && failCount > 3) {
                taskMultiply.clear();
            }
        }

        log.info("-------------------------------------------------");
        log.info("Закончили парсить, затраченное время: " + (new Date().getTime() - startTime) + " ms");

        return new ArrayList<>(result);
    }

    public static void closeClient() throws IOException {
        if (client != null)
            client.close();

        client = null;
    }
}
