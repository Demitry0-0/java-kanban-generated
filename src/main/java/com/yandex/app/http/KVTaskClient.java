package com.yandex.app.http;

import com.yandex.app.exceptions.ManagerSaveException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class KVTaskClient {
    private final String url;
    private final HttpClient client;
    private final String token;

    public KVTaskClient(String url) {
        this.url = url;
        this.client = HttpClient.newHttpClient();
        this.token = register();
    }

    private String register() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/register")).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException e) {
            throw new ManagerSaveException("Cannot register in KV server", e);
        }
    }

    public void put(String key, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/save/" + "?API_TOKEN=" + token + "&key=" + key))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            throw new ManagerSaveException("Cannot save to KV server", e);
        }
    }

    public String load(String key) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/load/" + "?API_TOKEN=" + token + "&key=" + key)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException e) {
            throw new ManagerSaveException("Cannot load from KV server", e);
        }
    }
}
