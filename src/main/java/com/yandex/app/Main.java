package com.yandex.app;

import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.model.TaskStatus;
import com.yandex.app.service.FileBackedTasksManager;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Path storage = Path.of("kanban.csv");

        FileBackedTasksManager manager = new FileBackedTasksManager(storage);

        Task task = new Task(
                0,
                "Prepare report",
                "Collect weekly metrics",
                TaskStatus.NEW,
                LocalDateTime.of(2026, 5, 20, 10, 0),
                Duration.ofMinutes(90)
        );

        Epic epic = new Epic(
                0,
                "Release 1.0",
                "Prepare release tasks",
                TaskStatus.NEW,
                LocalDateTime.of(2026, 5, 21, 9, 0),
                Duration.ZERO,
                List.of()
        );

        manager.createTask(task);
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask(
                0,
                "Smoke tests",
                "Run regression smoke suite",
                TaskStatus.NEW,
                LocalDateTime.of(2026, 5, 21, 11, 0),
                Duration.ofMinutes(60),
                epic.getId()
        );

        Subtask subtask2 = new Subtask(
                0,
                "Deploy",
                "Deploy release to production",
                TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 5, 21, 12, 30),
                Duration.ofMinutes(45),
                epic.getId()
        );

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        FileBackedTasksManager restored = FileBackedTasksManager.loadFromFile(storage);

        System.out.println("=== Restored tasks ===");
        for (Task restoredTask : restored.getTasks()) {
            System.out.println(restoredTask);
        }

        System.out.println("=== Restored epics ===");
        for (Epic restoredEpic : restored.getEpics()) {
            System.out.println(restoredEpic);
            System.out.println("  subtasks: " + restored.getEpicSubtasks(restoredEpic.getId()));
        }

        System.out.println("=== Prioritized ===");
        for (Task prioritized : restored.getPrioritizedTasks()) {
            System.out.println(prioritized);
        }
    }
}
