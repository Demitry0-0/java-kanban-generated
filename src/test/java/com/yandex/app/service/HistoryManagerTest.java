package com.yandex.app.service;

import com.yandex.app.model.Task;
import com.yandex.app.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryManagerTest {
    @Test
    void noDuplicatesAndOrder() {
        HistoryManager history = new InMemoryHistoryManager();
        Task t1 = new Task(1, "1", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,10,0), Duration.ofMinutes(5));
        Task t2 = new Task(2, "2", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,11,0), Duration.ofMinutes(5));
        history.add(t1); history.add(t2); history.add(t1);

        List<Task> list = history.getHistory();
        assertEquals(2, list.size());
        assertEquals(2, list.get(0).getId());
        assertEquals(1, list.get(1).getId());
    }

    @Test
    void removeByIdWorks() {
        HistoryManager history = new InMemoryHistoryManager();
        Task t1 = new Task(1, "1", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,10,0), Duration.ofMinutes(5));
        Task t2 = new Task(2, "2", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,11,0), Duration.ofMinutes(5));
        history.add(t1); history.add(t2);
        history.remove(1);
        assertEquals(1, history.getHistory().size());
        assertEquals(2, history.getHistory().get(0).getId());
    }
}
