package com.yandex.app;

import com.yandex.app.http.HttpTaskServer;
import com.yandex.app.http.KVServer;
import com.yandex.app.service.HttpTaskManager;

public class Main {
    public static void main(String[] args) throws Exception {
        KVServer kvServer = new KVServer();
        kvServer.start();

        HttpTaskManager manager = new HttpTaskManager("http://localhost:8078");
        manager.load();

        HttpTaskServer httpTaskServer = new HttpTaskServer(manager);
        httpTaskServer.start();

        System.out.println("KVServer started on 8078");
        System.out.println("HttpTaskServer started on 8080");
    }
}
