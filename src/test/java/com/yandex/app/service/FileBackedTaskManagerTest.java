package com.yandex.app.service;

import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {
    @Test
    void saveLoadAndLinksRestored() throws Exception {
        Path file = Files.createTempFile("kanban-test", ".csv");
        try {
            FileBackedTasksManager manager = new FileBackedTasksManager(file);
            Task t = manager.createTask(new Task(0, "t", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,10,0), Duration.ofMinutes(10)));
            Epic e = manager.createEpic(new Epic(0, "e", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,11,0), Duration.ZERO, List.of()));
            Subtask s = manager.createSubtask(new Subtask(0, "s", "d", TaskStatus.DONE, LocalDateTime.of(2026,1,1,12,0), Duration.ofMinutes(20), e.getId()));

            FileBackedTasksManager loaded = FileBackedTasksManager.loadFromFile(file);
            assertNotNull(loaded.getTaskById(t.getId()));
            assertNotNull(loaded.getEpicById(e.getId()));
            assertNotNull(loaded.getSubtaskById(s.getId()));
            assertEquals(1, loaded.getEpicSubtasks(e.getId()).size());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
