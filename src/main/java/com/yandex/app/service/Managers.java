package com.yandex.app.service;

import java.nio.file.Path;

public final class Managers {
    private Managers() {
    }

    public static TaskManager getDefaultTaskManager() {
        return new InMemoryTaskManager(getDefaultHistory());
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }

    public static FileBackedTasksManager getFileBackedManager(Path path) {
        return new FileBackedTasksManager(path);
    }
}
