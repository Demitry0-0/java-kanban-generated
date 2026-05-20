package com.yandex.app.model;

public enum TaskType {
    TASK,
    EPIC,
    SUBTASK;

    public static TaskType fromString(String value) {
        return TaskType.valueOf(value.trim().toUpperCase());
    }
}
