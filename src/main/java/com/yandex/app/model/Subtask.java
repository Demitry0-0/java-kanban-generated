package com.yandex.app.model;

import com.yandex.app.exceptions.ValidationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class Subtask extends Task {
    private int epicId;

    public Subtask(int id,
                   String name,
                   String description,
                   TaskStatus status,
                   LocalDateTime startTime,
                   Duration duration,
                   int epicId) {
        super(id, name, description, status, startTime, duration);
        this.epicId = epicId;
        validateSubtask();
    }

    private void validateSubtask() {
        if (epicId < 0) {
            throw new ValidationException("epicId must be non-negative");
        }
    }

    @Override
    public TaskType getType() {
        return TaskType.SUBTASK;
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
        validateSubtask();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        Subtask subtask = (Subtask) o;
        return epicId == subtask.epicId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), epicId);
    }

    @Override
    public String toString() {
        return "Subtask{" +
                "base=" + super.toString() +
                ", epicId=" + epicId +
                '}';
    }
}
