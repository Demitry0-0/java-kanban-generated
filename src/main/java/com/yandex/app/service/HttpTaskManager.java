package com.yandex.app.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yandex.app.http.KVTaskClient;
import com.yandex.app.http.LocalDateAdapter;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class HttpTaskManager extends InMemoryTaskManager {
    private static final String TASKS_KEY = "tasks";
    private static final String EPICS_KEY = "epics";
    private static final String SUBTASKS_KEY = "subtasks";

    private final KVTaskClient client;
    private final Gson gson;

    public HttpTaskManager(String url) {
        this.client = new KVTaskClient(url);
        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateAdapter()).create();
    }

    public void load() {
        Type taskListType = new TypeToken<List<Task>>() {
        }.getType();
        Type epicListType = new TypeToken<List<Epic>>() {
        }.getType();
        Type subtaskListType = new TypeToken<List<Subtask>>() {
        }.getType();

        List<Task> loadedTasks = parse(client.load(TASKS_KEY), taskListType);
        List<Epic> loadedEpics = parse(client.load(EPICS_KEY), epicListType);
        List<Subtask> loadedSubtasks = parse(client.load(SUBTASKS_KEY), subtaskListType);

        tasks.clear();
        epics.clear();
        subtasks.clear();

        int maxId = 0;
        for (Task task : loadedTasks) {
            putLoadedTask(task);
            maxId = Math.max(maxId, task.getId());
        }
        for (Epic epic : loadedEpics) {
            putLoadedEpic(epic);
            maxId = Math.max(maxId, epic.getId());
        }
        for (Subtask subtask : loadedSubtasks) {
            putLoadedSubtask(subtask);
            maxId = Math.max(maxId, subtask.getId());
        }

        setNextId(maxId + 1);
        rebuildEpics();
    }

    @Override
    public Task createTask(Task task) {
        return withSave(() -> super.createTask(task));
    }

    @Override
    public Task updateTask(Task task) {
        return withSave(() -> super.updateTask(task));
    }

    @Override
    public void deleteTaskById(int id) {
        withSave(() -> super.deleteTaskById(id));
    }

    @Override
    public void deleteAllTasks() {
        withSave(super::deleteAllTasks);
    }

    @Override
    public Epic createEpic(Epic epic) {
        return withSave(() -> super.createEpic(epic));
    }

    @Override
    public Epic updateEpic(Epic epic) {
        return withSave(() -> super.updateEpic(epic));
    }

    @Override
    public void deleteEpicById(int id) {
        withSave(() -> super.deleteEpicById(id));
    }

    @Override
    public void deleteAllEpics() {
        withSave(super::deleteAllEpics);
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        return withSave(() -> super.createSubtask(subtask));
    }

    @Override
    public Subtask updateSubtask(Subtask subtask) {
        return withSave(() -> super.updateSubtask(subtask));
    }

    @Override
    public void deleteSubtaskById(int id) {
        withSave(() -> super.deleteSubtaskById(id));
    }

    @Override
    public void deleteAllSubtasks() {
        withSave(super::deleteAllSubtasks);
    }

    private <T> T withSave(Supplier<T> supplier) {
        T value = supplier.get();
        save();
        return value;
    }

    private void withSave(Runnable runnable) {
        runnable.run();
        save();
    }

    private <T> List<T> parse(String json, Type type) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        List<T> result = gson.fromJson(json, type);
        return result == null ? new ArrayList<>() : result;
    }

    private void save() {
        client.put(TASKS_KEY, gson.toJson(getTasks()));
        client.put(EPICS_KEY, gson.toJson(getEpics()));
        client.put(SUBTASKS_KEY, gson.toJson(getSubtasks()));
    }
}
