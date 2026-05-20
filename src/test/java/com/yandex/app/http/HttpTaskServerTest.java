package com.yandex.app.http;

import com.yandex.app.service.HttpTaskManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpTaskServerTest {
    private KVServer kvServer;
    private HttpTaskServer taskServer;
    private HttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        kvServer = new KVServer();
        kvServer.start();
        HttpTaskManager manager = new HttpTaskManager("http://localhost:8078");
        manager.load();
        taskServer = new HttpTaskServer(manager);
        taskServer.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        taskServer.stop();
        kvServer.stop();
    }

    @Test
    void postGetDeleteTaskAndStatusCodes() throws IOException, InterruptedException {
        String payload = "{\"id\":0,\"name\":\"t\",\"description\":\"d\",\"status\":\"NEW\",\"startTime\":\"2026-01-01T10:00\",\"duration\":\"PT30M\"}";
        HttpResponse<String> post = req("POST", "http://localhost:8080/tasks/task", payload);
        assertEquals(201, post.statusCode());

        HttpResponse<String> getAll = req("GET", "http://localhost:8080/tasks/task", null);
        assertEquals(200, getAll.statusCode());

        HttpResponse<String> getById = req("GET", "http://localhost:8080/tasks/task?id=1", null);
        assertEquals(200, getById.statusCode());

        HttpResponse<String> badId = req("GET", "http://localhost:8080/tasks/task?id=abc", null);
        assertEquals(400, badId.statusCode());

        HttpResponse<String> notFound = req("GET", "http://localhost:8080/tasks/unknown", null);
        assertEquals(404, notFound.statusCode());

        HttpResponse<String> del = req("DELETE", "http://localhost:8080/tasks/task?id=1", null);
        assertEquals(200, del.statusCode());
    }

    @Test
    void historyAndPrioritizedEndpoints() throws IOException, InterruptedException {
        String t1 = "{\"id\":0,\"name\":\"a\",\"description\":\"d\",\"status\":\"NEW\",\"startTime\":\"2026-01-01T09:00\",\"duration\":\"PT30M\"}";
        String t2 = "{\"id\":0,\"name\":\"b\",\"description\":\"d\",\"status\":\"NEW\",\"startTime\":\"2026-01-01T10:00\",\"duration\":\"PT30M\"}";
        req("POST", "http://localhost:8080/tasks/task", t1);
        req("POST", "http://localhost:8080/tasks/task", t2);

        req("GET", "http://localhost:8080/tasks/task?id=1", null);
        req("GET", "http://localhost:8080/tasks/task?id=2", null);

        HttpResponse<String> history = req("GET", "http://localhost:8080/tasks/history", null);
        assertEquals(200, history.statusCode());

        HttpResponse<String> prioritized = req("GET", "http://localhost:8080/tasks/prioritized", null);
        assertEquals(200, prioritized.statusCode());
    }

    private HttpResponse<String> req(String method, String url, String body) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url));
        if ("POST".equals(method)) {
            b.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else if ("DELETE".equals(method)) {
            b.DELETE();
        } else {
            b.GET();
        }
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
