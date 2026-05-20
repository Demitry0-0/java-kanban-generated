package com.yandex.app.service;

import com.yandex.app.http.KVServer;
import com.yandex.app.model.Task;
import com.yandex.app.model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HttpTaskManagerTest {
    private KVServer kvServer;

    @BeforeEach
    void start() throws Exception {
        kvServer = new KVServer();
        kvServer.start();
    }

    @AfterEach
    void stop() {
        kvServer.stop();
    }

    @Test
    void persistsToKvAndLoads() {
        HttpTaskManager m1 = new HttpTaskManager("http://localhost:8078");
        Task created = m1.createTask(new Task(0, "t", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,10,0), Duration.ofMinutes(15)));

        HttpTaskManager m2 = new HttpTaskManager("http://localhost:8078");
        m2.load();
        assertNotNull(m2.getTaskById(created.getId()));
        assertEquals("t", m2.getTaskById(created.getId()).getName());
    }
}
