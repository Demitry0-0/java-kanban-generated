package com.yandex.app.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KVServer {
    public static final int PORT = 8078;
    private final HttpServer server;
    private final Gson gson;
    private final Map<String, String> data = new HashMap<>();
    private final String apiToken = UUID.randomUUID().toString();

    public KVServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateAdapter()).create();
        server.createContext("/register", this::handleRegister);
        server.createContext("/save", this::handleSave);
        server.createContext("/load", this::handleLoad);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "");
            return;
        }
        send(exchange, 200, apiToken);
    }

    private void handleSave(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "");
            return;
        }
        String query = exchange.getRequestURI().getQuery();
        String key = extract(query, "key");
        String token = extract(query, "API_TOKEN");
        if (key == null || !apiToken.equals(token)) {
            send(exchange, 403, "Forbidden");
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        data.put(key, body);
        send(exchange, 200, "");
    }

    private void handleLoad(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "");
            return;
        }
        String query = exchange.getRequestURI().getQuery();
        String key = extract(query, "key");
        String token = extract(query, "API_TOKEN");
        if (key == null || !apiToken.equals(token)) {
            send(exchange, 403, "Forbidden");
            return;
        }
        send(exchange, 200, data.getOrDefault(key, ""));
    }

    private static String extract(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String p : query.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

    private void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
