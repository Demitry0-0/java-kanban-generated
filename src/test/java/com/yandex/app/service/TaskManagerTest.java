package com.yandex.app.service;

import com.yandex.app.exceptions.ValidationException;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerTest {
    private InMemoryTaskManager manager;

    @BeforeEach
    void setUp() { manager = new InMemoryTaskManager(); }

    @Test
    void taskCrudWorks() {
        Task task = new Task(0, "t", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,10,0), Duration.ofMinutes(30));
        Task created = manager.createTask(task);
        assertTrue(created.getId() > 0);
        assertEquals(created, manager.getTaskById(created.getId()));

        created.setName("updated");
        manager.updateTask(created);
        assertEquals("updated", manager.getTaskById(created.getId()).getName());

        manager.deleteTaskById(created.getId());
        assertNull(manager.getTaskById(created.getId()));
    }

    @Test
    void epicStatusCalculatedFromSubtasks() {
        Epic epic = manager.createEpic(new Epic(0, "e", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,9,0), Duration.ZERO, List.of()));
        assertEquals(TaskStatus.NEW, manager.getEpicById(epic.getId()).getStatus());

        Subtask s1 = manager.createSubtask(new Subtask(0, "s1", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,10,0), Duration.ofMinutes(30), epic.getId()));
        Subtask s2 = manager.createSubtask(new Subtask(0, "s2", "d", TaskStatus.DONE, LocalDateTime.of(2026,1,1,11,0), Duration.ofMinutes(30), epic.getId()));
        assertEquals(TaskStatus.IN_PROGRESS, manager.getEpicById(epic.getId()).getStatus());

        s1.setStatus(TaskStatus.DONE);
        manager.updateSubtask(s1);
        assertEquals(TaskStatus.DONE, manager.getEpicById(epic.getId()).getStatus());
        assertNotNull(s2);
    }

    @Test
    void rejectsTimeIntersections() {
        manager.createTask(new Task(0, "a", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,10,0), Duration.ofMinutes(60)));
        Task crossing = new Task(0, "b", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,10,30), Duration.ofMinutes(30));
        assertThrows(ValidationException.class, () -> manager.createTask(crossing));
    }

    @Test
    void prioritizedByStartTime() {
        Task t2 = manager.createTask(new Task(0, "late", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,12,0), Duration.ofMinutes(30)));
        Task t1 = manager.createTask(new Task(0, "early", "d", TaskStatus.NEW, LocalDateTime.of(2026,1,1,9,0), Duration.ofMinutes(30)));
        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(t1.getId(), prioritized.get(0).getId());
        assertEquals(t2.getId(), prioritized.get(1).getId());
    }
}
