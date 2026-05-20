package com.yandex.app.service;

import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;

import java.util.List;

public interface TaskManager {
    Task createTask(Task task);

    Task updateTask(Task task);

    Task getTaskById(int id);

    List<Task> getTasks();

    void deleteTaskById(int id);

    void deleteAllTasks();

    Epic createEpic(Epic epic);

    Epic updateEpic(Epic epic);

    Epic getEpicById(int id);

    List<Epic> getEpics();

    void deleteEpicById(int id);

    void deleteAllEpics();

    Subtask createSubtask(Subtask subtask);

    Subtask updateSubtask(Subtask subtask);

    Subtask getSubtaskById(int id);

    List<Subtask> getSubtasks();

    List<Subtask> getEpicSubtasks(int epicId);

    void deleteSubtaskById(int id);

    void deleteAllSubtasks();

    List<Task> getHistory();

    List<Task> getPrioritizedTasks();

    void validate(Task task);

    void checkEpicStatus(int epicId);

    void setEpicDateTime(int epicId);
}
