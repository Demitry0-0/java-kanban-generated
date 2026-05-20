package com.yandex.app.service;

import com.yandex.app.exceptions.ValidationException;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.model.TaskStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryTaskManager implements TaskManager {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final HistoryManager historyManager;
    private int nextId = 1;

    public InMemoryTaskManager() {
        this(new InMemoryHistoryManager());
    }

    public InMemoryTaskManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    @Override
    public Task createTask(Task task) {
        validate(task);
        int id = generateId();
        task.setId(id);
        tasks.put(id, task);
        return task;
    }

    @Override
    public Task updateTask(Task task) {
        if (task == null || !tasks.containsKey(task.getId())) {
            throw new ValidationException("Task to update does not exist");
        }
        validate(task);
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        historyManager.add(task);
        return task;
    }

    @Override
    public List<Task> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void deleteTaskById(int id) {
        tasks.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void deleteAllTasks() {
        for (Integer id : new ArrayList<>(tasks.keySet())) {
            historyManager.remove(id);
        }
        tasks.clear();
    }

    @Override
    public Epic createEpic(Epic epic) {
        if (epic == null) {
            throw new ValidationException("Epic must not be null");
        }
        int id = generateId();
        epic.setId(id);
        epics.put(id, epic);
        checkEpicStatus(id);
        setEpicDateTime(id);
        return epic;
    }

    @Override
    public Epic updateEpic(Epic epic) {
        if (epic == null || !epics.containsKey(epic.getId())) {
            throw new ValidationException("Epic to update does not exist");
        }
        Epic stored = epics.get(epic.getId());
        stored.setName(epic.getName());
        stored.setDescription(epic.getDescription());
        checkEpicStatus(stored.getId());
        setEpicDateTime(stored.getId());
        return stored;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        historyManager.add(epic);
        return epic;
    }

    @Override
    public List<Epic> getEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void deleteEpicById(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (Integer subtaskId : new ArrayList<>(epic.getSubtaskIds())) {
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteAllEpics() {
        for (Integer subtaskId : new ArrayList<>(subtasks.keySet())) {
            historyManager.remove(subtaskId);
        }
        subtasks.clear();

        for (Integer epicId : new ArrayList<>(epics.keySet())) {
            historyManager.remove(epicId);
        }
        epics.clear();
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        if (subtask == null) {
            throw new ValidationException("Subtask must not be null");
        }
        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) {
            throw new ValidationException("Epic for subtask does not exist");
        }
        validate(subtask);
        int id = generateId();
        subtask.setId(id);
        subtasks.put(id, subtask);
        epic.addSubtaskId(id);
        checkEpicStatus(epic.getId());
        setEpicDateTime(epic.getId());
        return subtask;
    }

    @Override
    public Subtask updateSubtask(Subtask subtask) {
        if (subtask == null || !subtasks.containsKey(subtask.getId())) {
            throw new ValidationException("Subtask to update does not exist");
        }
        if (!epics.containsKey(subtask.getEpicId())) {
            throw new ValidationException("Epic for subtask does not exist");
        }
        validate(subtask);
        Subtask previous = subtasks.get(subtask.getId());
        if (previous.getEpicId() != subtask.getEpicId()) {
            Epic oldEpic = epics.get(previous.getEpicId());
            oldEpic.removeSubtaskId(previous.getId());
            checkEpicStatus(oldEpic.getId());
            setEpicDateTime(oldEpic.getId());

            Epic newEpic = epics.get(subtask.getEpicId());
            newEpic.addSubtaskId(subtask.getId());
        }
        subtasks.put(subtask.getId(), subtask);
        checkEpicStatus(subtask.getEpicId());
        setEpicDateTime(subtask.getEpicId());
        return subtask;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        historyManager.add(subtask);
        return subtask;
    }

    @Override
    public List<Subtask> getSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public List<Subtask> getEpicSubtasks(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            throw new ValidationException("Epic not found");
        }
        List<Subtask> result = new ArrayList<>();
        for (Integer subtaskId : epic.getSubtaskIds()) {
            Subtask subtask = subtasks.get(subtaskId);
            if (subtask != null) {
                result.add(subtask);
            }
        }
        return result;
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask removed = subtasks.remove(id);
        if (removed != null) {
            Epic epic = epics.get(removed.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(id);
                checkEpicStatus(epic.getId());
                setEpicDateTime(epic.getId());
            }
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteAllSubtasks() {
        for (Subtask subtask : new ArrayList<>(subtasks.values())) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(subtask.getId());
            }
            historyManager.remove(subtask.getId());
        }
        subtasks.clear();
        for (Integer epicId : epics.keySet()) {
            checkEpicStatus(epicId);
            setEpicDateTime(epicId);
        }
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(tasks.values());
        allTasks.addAll(subtasks.values());
        allTasks.sort(Comparator.comparing(Task::getStartTime));
        return allTasks;
    }

    @Override
    public void validate(Task task) {
        if (task == null) {
            throw new ValidationException("Task must not be null");
        }
        for (Task existingTask : getPrioritizedTasks()) {
            if (existingTask.getId() == task.getId()) {
                continue;
            }
            if (isCrossed(task, existingTask)) {
                throw new ValidationException("Task time intersection detected: " + task.getId());
            }
        }
    }

    @Override
    public void checkEpicStatus(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            throw new ValidationException("Epic not found");
        }
        List<Subtask> epicSubtasks = getEpicSubtasks(epicId);
        if (epicSubtasks.isEmpty()) {
            epic.setStatus(TaskStatus.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (Subtask subtask : epicSubtasks) {
            if (subtask.getStatus() != TaskStatus.NEW) {
                allNew = false;
            }
            if (subtask.getStatus() != TaskStatus.DONE) {
                allDone = false;
            }
        }

        if (allDone) {
            epic.setStatus(TaskStatus.DONE);
        } else if (allNew) {
            epic.setStatus(TaskStatus.NEW);
        } else {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        }
    }

    @Override
    public void setEpicDateTime(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            throw new ValidationException("Epic not found");
        }
        List<Subtask> epicSubtasks = getEpicSubtasks(epicId);
        if (epicSubtasks.isEmpty()) {
            epic.setStartTime(LocalDateTime.MIN);
            epic.setDuration(Duration.ZERO);
            return;
        }

        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;
        Duration total = Duration.ZERO;

        for (Subtask subtask : epicSubtasks) {
            if (minStart == null || subtask.getStartTime().isBefore(minStart)) {
                minStart = subtask.getStartTime();
            }
            LocalDateTime end = subtask.getEndTime();
            if (maxEnd == null || end.isAfter(maxEnd)) {
                maxEnd = end;
            }
            total = total.plus(subtask.getDuration());
        }

        epic.setStartTime(minStart);
        epic.setDuration(total);
    }

    private int generateId() {
        return nextId++;
    }

    private boolean isCrossed(Task first, Task second) {
        LocalDateTime firstStart = first.getStartTime();
        LocalDateTime firstEnd = first.getEndTime();
        LocalDateTime secondStart = second.getStartTime();
        LocalDateTime secondEnd = second.getEndTime();

        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }
}
