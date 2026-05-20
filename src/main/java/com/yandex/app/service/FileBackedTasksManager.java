package com.yandex.app.service;

import com.yandex.app.exceptions.ManagerSaveException;
import com.yandex.app.exceptions.ValidationException;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.model.TaskStatus;
import com.yandex.app.model.TaskType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileBackedTasksManager extends InMemoryTaskManager {
    private static final String HEADER = "id,type,name,status,description,startTime,duration,epicId";
    private final Path file;

    public FileBackedTasksManager(Path file) {
        super();
        this.file = file;
    }

    public static FileBackedTasksManager loadFromFile(Path file) {
        FileBackedTasksManager manager = new FileBackedTasksManager(file);
        if (!Files.exists(file)) {
            return manager;
        }

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return manager;
            }

            int maxId = 0;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }
                Task task = fromCsv(line);
                if (task.getId() > maxId) {
                    maxId = task.getId();
                }

                if (task instanceof Epic) {
                    manager.putLoadedEpic((Epic) task);
                } else if (task instanceof Subtask) {
                    manager.putLoadedSubtask((Subtask) task);
                } else {
                    manager.putLoadedTask(task);
                }
            }
            manager.setNextId(maxId + 1);
            manager.rebuildEpics();
            return manager;
        } catch (IOException e) {
            throw new ManagerSaveException("Failed to load manager from file: " + file, e);
        }
    }

    @Override
    public Task createTask(Task task) {
        Task created = super.createTask(task);
        save();
        return created;
    }

    @Override
    public Task updateTask(Task task) {
        Task updated = super.updateTask(task);
        save();
        return updated;
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public Epic createEpic(Epic epic) {
        Epic created = super.createEpic(epic);
        save();
        return created;
    }

    @Override
    public Epic updateEpic(Epic epic) {
        Epic updated = super.updateEpic(epic);
        save();
        return updated;
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        Subtask created = super.createSubtask(subtask);
        save();
        return created;
    }

    @Override
    public Subtask updateSubtask(Subtask subtask) {
        Subtask updated = super.updateSubtask(subtask);
        save();
        return updated;
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    @Override
    public void deleteAllSubtasks() {
        super.deleteAllSubtasks();
        save();
    }

    private void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            List<Task> all = new ArrayList<>();
            all.addAll(getTasks());
            all.addAll(getEpics());
            all.addAll(getSubtasks());
            all.sort(Comparator.comparingInt(Task::getId));

            for (Task task : all) {
                writer.write(toCsv(task));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Failed to save tasks to file: " + file, e);
        }
    }

    private static String toCsv(Task task) {
        String base = task.getId() + ","
                + task.getType() + ","
                + escape(task.getName()) + ","
                + task.getStatus() + ","
                + escape(task.getDescription()) + ","
                + task.getStartTime() + ","
                + task.getDuration();

        if (task instanceof Subtask) {
            return base + "," + ((Subtask) task).getEpicId();
        }
        return base + ",";
    }

    private static Task fromCsv(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 8) {
            throw new ValidationException("Invalid CSV line: " + line);
        }

        int id = Integer.parseInt(parts[0]);
        TaskType type = TaskType.fromString(parts[1]);
        String name = unescape(parts[2]);
        TaskStatus status = TaskStatus.valueOf(parts[3]);
        String description = unescape(parts[4]);
        LocalDateTime startTime = LocalDateTime.parse(parts[5]);
        Duration duration = Duration.parse(parts[6]);

        switch (type) {
            case TASK:
                return new Task(id, name, description, status, startTime, duration);
            case EPIC:
                return new Epic(id, name, description, status, startTime, duration, new ArrayList<>());
            case SUBTASK:
                int epicId = Integer.parseInt(parts[7]);
                return new Subtask(id, name, description, status, startTime, duration, epicId);
            default:
                throw new ValidationException("Unsupported task type: " + type);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace(",", "\\,");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
