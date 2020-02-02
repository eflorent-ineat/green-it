package ineat.demo;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class StorageClient {


    CookieManager cookieManager;
    HttpClient httpClient;
    String server;

    public StorageClient(String server) {
        this.server = server;
        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(cookieManager)
                .build();
    }

    public void demo() throws IOException, InterruptedException {
        HttpResponse<String> response;
        HttpRequest request;
        request = HttpRequest.newBuilder()
                .GET()
                .header("Accept", "text/html")
                .uri(URI.create(server))
                .build();
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
    }

}
