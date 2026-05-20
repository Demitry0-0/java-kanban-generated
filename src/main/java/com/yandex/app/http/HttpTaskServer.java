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

    public void start() { server.start(); }
    public void stop() { server.stop(0); }

    private void handle(HttpExchange e) throws IOException {
        Endpoint ep = resolve(e);
        try {
            switch (ep) {
                case GET_TASKS: send(e,200,gson.toJson(manager.getTasks())); break;
                case GET_TASK_BY_ID: respondTaskById(e); break;
                case POST_TASK: manager.createTask(gson.fromJson(body(e), Task.class)); send(e,201,""); break;
                case DELETE_TASK_BY_ID: manager.deleteTaskById(id(e)); send(e,200,""); break;
                case DELETE_TASKS: manager.deleteAllTasks(); send(e,200,""); break;
                case GET_EPICS: send(e,200,gson.toJson(manager.getEpics())); break;
                case GET_EPIC_BY_ID: respondEpicById(e); break;
                case POST_EPIC: manager.createEpic(gson.fromJson(body(e), Epic.class)); send(e,201,""); break;
                case DELETE_EPIC_BY_ID: manager.deleteEpicById(id(e)); send(e,200,""); break;
                case DELETE_EPICS: manager.deleteAllEpics(); send(e,200,""); break;
                case GET_EPIC_SUBTASKS: send(e,200,gson.toJson(manager.getEpicSubtasks(id(e)))); break;
                case GET_SUBTASKS: send(e,200,gson.toJson(manager.getSubtasks())); break;
                case GET_SUBTASK_BY_ID: respondSubtaskById(e); break;
                case POST_SUBTASK: manager.createSubtask(gson.fromJson(body(e), Subtask.class)); send(e,201,""); break;
                case DELETE_SUBTASK_BY_ID: manager.deleteSubtaskById(id(e)); send(e,200,""); break;
                case DELETE_SUBTASKS: manager.deleteAllSubtasks(); send(e,200,""); break;
                case GET_HISTORY: send(e,200,gson.toJson(manager.getHistory())); break;
                case GET_PRIORITIZED: send(e,200,gson.toJson(manager.getPrioritizedTasks())); break;
                default: send(e,404,"Not found");
            }
        } catch (NumberFormatException ex) {
            send(e,400,"Invalid id");
        } catch (RuntimeException ex) {
            send(e,400,ex.getMessage());
        }
    }

    private void respondTaskById(HttpExchange e) throws IOException { Task t=manager.getTaskById(id(e)); if(t==null) send(e,404,""); else send(e,200,gson.toJson(t)); }
    private void respondEpicById(HttpExchange e) throws IOException { Epic t=manager.getEpicById(id(e)); if(t==null) send(e,404,""); else send(e,200,gson.toJson(t)); }
    private void respondSubtaskById(HttpExchange e) throws IOException { Subtask t=manager.getSubtaskById(id(e)); if(t==null) send(e,404,""); else send(e,200,gson.toJson(t)); }

    private Endpoint resolve(HttpExchange e) {
        String method = e.getRequestMethod();
        String path = e.getRequestURI().getPath();
        String[] p = path.split("/");
        if (p.length < 3) return Endpoint.NOT_FOUND;
        if (!"tasks".equals(p[1])) return Endpoint.NOT_FOUND;
        String entity = p[2];
        boolean hasId = queryParam(e, "id") != null;

        switch (entity) {
            case "task":
                if ("GET".equals(method)) return hasId ? Endpoint.GET_TASK_BY_ID : Endpoint.GET_TASKS;
                if ("POST".equals(method)) return Endpoint.POST_TASK;
                if ("DELETE".equals(method)) return hasId ? Endpoint.DELETE_TASK_BY_ID : Endpoint.DELETE_TASKS;
                break;
            case "epic":
                if ("GET".equals(method)) return hasId ? Endpoint.GET_EPIC_BY_ID : Endpoint.GET_EPICS;
                if ("POST".equals(method)) return Endpoint.POST_EPIC;
                if ("DELETE".equals(method)) return hasId ? Endpoint.DELETE_EPIC_BY_ID : Endpoint.DELETE_EPICS;
                break;
            case "subtask":
                if ("GET".equals(method)) return hasId ? Endpoint.GET_SUBTASK_BY_ID : Endpoint.GET_SUBTASKS;
                if ("POST".equals(method)) return Endpoint.POST_SUBTASK;
                if ("DELETE".equals(method)) return hasId ? Endpoint.DELETE_SUBTASK_BY_ID : Endpoint.DELETE_SUBTASKS;
                break;
            case "history": if ("GET".equals(method)) return Endpoint.GET_HISTORY; break;
            case "prioritized": if ("GET".equals(method)) return Endpoint.GET_PRIORITIZED; break;
            case "epic-subtasks": if ("GET".equals(method) && hasId) return Endpoint.GET_EPIC_SUBTASKS; break;
            default: break;
        }
        return Endpoint.NOT_FOUND;
    }

    private int id(HttpExchange e) { return Integer.parseInt(queryParam(e, "id")); }
    private String body(HttpExchange e) throws IOException { return new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); }
    private String queryParam(HttpExchange e, String k){ String q=e.getRequestURI().getQuery(); if(q==null) return null; for(String p:q.split("&")){String[] kv=p.split("=",2); if(kv.length==2&&kv[0].equals(k)) return kv[1];} return null; }
    private void send(HttpExchange e, int code, String body) throws IOException { byte[] b=body.getBytes(StandardCharsets.UTF_8); e.sendResponseHeaders(code,b.length); e.getResponseBody().write(b); e.close(); }
}
