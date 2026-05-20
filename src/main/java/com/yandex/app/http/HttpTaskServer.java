package com.yandex.app.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.service.HttpTaskManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.function.IntFunction;

public class HttpTaskServer {
    private final HttpServer server;
    private final HttpTaskManager manager;
    private final Gson gson;

    public HttpTaskServer(HttpTaskManager manager) throws IOException {
        this.manager = manager;
        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateAdapter()).create();
        this.server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", this::handle);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        Endpoint endpoint = resolve(exchange);
        try {
            switch (endpoint) {
                case GET_TASKS:
                    sendJson(exchange, 200, manager.getTasks());
                    break;
                case GET_TASK_BY_ID:
                    respondById(exchange, manager::getTaskById);
                    break;
                case POST_TASK:
                    manager.createTask(gson.fromJson(body(exchange), Task.class));
                    send(exchange, 201, "");
                    break;
                case DELETE_TASK_BY_ID:
                    manager.deleteTaskById(id(exchange));
                    send(exchange, 200, "");
                    break;
                case DELETE_TASKS:
                    manager.deleteAllTasks();
                    send(exchange, 200, "");
                    break;
                case GET_EPICS:
                    sendJson(exchange, 200, manager.getEpics());
                    break;
                case GET_EPIC_BY_ID:
                    respondById(exchange, manager::getEpicById);
                    break;
                case POST_EPIC:
                    manager.createEpic(gson.fromJson(body(exchange), Epic.class));
                    send(exchange, 201, "");
                    break;
                case DELETE_EPIC_BY_ID:
                    manager.deleteEpicById(id(exchange));
                    send(exchange, 200, "");
                    break;
                case DELETE_EPICS:
                    manager.deleteAllEpics();
                    send(exchange, 200, "");
                    break;
                case GET_EPIC_SUBTASKS:
                    sendJson(exchange, 200, manager.getEpicSubtasks(id(exchange)));
                    break;
                case GET_SUBTASKS:
                    sendJson(exchange, 200, manager.getSubtasks());
                    break;
                case GET_SUBTASK_BY_ID:
                    respondById(exchange, manager::getSubtaskById);
                    break;
                case POST_SUBTASK:
                    manager.createSubtask(gson.fromJson(body(exchange), Subtask.class));
                    send(exchange, 201, "");
                    break;
                case DELETE_SUBTASK_BY_ID:
                    manager.deleteSubtaskById(id(exchange));
                    send(exchange, 200, "");
                    break;
                case DELETE_SUBTASKS:
                    manager.deleteAllSubtasks();
                    send(exchange, 200, "");
                    break;
                case GET_HISTORY:
                    sendJson(exchange, 200, manager.getHistory());
                    break;
                case GET_PRIORITIZED:
                    sendJson(exchange, 200, manager.getPrioritizedTasks());
                    break;
                default:
                    send(exchange, 404, "Not found");
            }
        } catch (NumberFormatException ex) {
            send(exchange, 400, "Invalid id");
        } catch (RuntimeException ex) {
            send(exchange, 400, ex.getMessage());
        }
    }

    private Endpoint resolve(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 3 || !"tasks".equals(parts[1])) {
            return Endpoint.NOT_FOUND;
        }

        String entity = parts[2];
        boolean hasId = queryParam(exchange, "id") != null;

        switch (entity) {
            case "task":
                if ("GET".equals(method)) {
                    return hasId ? Endpoint.GET_TASK_BY_ID : Endpoint.GET_TASKS;
                }
                if ("POST".equals(method)) {
                    return Endpoint.POST_TASK;
                }
                if ("DELETE".equals(method)) {
                    return hasId ? Endpoint.DELETE_TASK_BY_ID : Endpoint.DELETE_TASKS;
                }
                break;
            case "epic":
                if ("GET".equals(method)) {
                    return hasId ? Endpoint.GET_EPIC_BY_ID : Endpoint.GET_EPICS;
                }
                if ("POST".equals(method)) {
                    return Endpoint.POST_EPIC;
                }
                if ("DELETE".equals(method)) {
                    return hasId ? Endpoint.DELETE_EPIC_BY_ID : Endpoint.DELETE_EPICS;
                }
                break;
            case "subtask":
                if ("GET".equals(method)) {
                    return hasId ? Endpoint.GET_SUBTASK_BY_ID : Endpoint.GET_SUBTASKS;
                }
                if ("POST".equals(method)) {
                    return Endpoint.POST_SUBTASK;
                }
                if ("DELETE".equals(method)) {
                    return hasId ? Endpoint.DELETE_SUBTASK_BY_ID : Endpoint.DELETE_SUBTASKS;
                }
                break;
            case "history":
                if ("GET".equals(method)) {
                    return Endpoint.GET_HISTORY;
                }
                break;
            case "prioritized":
                if ("GET".equals(method)) {
                    return Endpoint.GET_PRIORITIZED;
                }
                break;
            case "epic-subtasks":
                if ("GET".equals(method) && hasId) {
                    return Endpoint.GET_EPIC_SUBTASKS;
                }
                break;
            default:
                break;
        }
        return Endpoint.NOT_FOUND;
    }

    private void respondById(HttpExchange exchange, IntFunction<Object> finder) throws IOException {
        Object entity = finder.apply(id(exchange));
        if (entity == null) {
            send(exchange, 404, "");
            return;
        }
        sendJson(exchange, 200, entity);
    }

    private int id(HttpExchange exchange) {
        return Integer.parseInt(queryParam(exchange, "id"));
    }

    private String body(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String queryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

    private void sendJson(HttpExchange exchange, int code, Object body) throws IOException {
        send(exchange, code, gson.toJson(body));
    }

    private void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
