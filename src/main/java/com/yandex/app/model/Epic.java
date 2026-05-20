package com.yandex.app.model;

import com.yandex.app.exceptions.ValidationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Epic extends Task {
    private final List<Integer> subtaskIds;

    public Epic(int id,
                String name,
                String description,
                TaskStatus status,
                LocalDateTime startTime,
                Duration duration,
                List<Integer> subtaskIds) {
        super(id, name, description, status, startTime, duration);
        this.subtaskIds = new ArrayList<>();
        if (subtaskIds != null) {
            for (Integer subtaskId : subtaskIds) {
                addSubtaskId(subtaskId);
            }
        }
    }

    @Override
    public TaskType getType() {
        return TaskType.EPIC;
    }

    public List<Integer> getSubtaskIds() {
        return Collections.unmodifiableList(subtaskIds);
    }

    public void addSubtaskId(Integer subtaskId) {
        if (subtaskId == null || subtaskId < 0) {
            throw new ValidationException("Subtask id must be non-null and non-negative");
        }
        subtaskIds.add(subtaskId);
    }

    public boolean removeSubtaskId(Integer subtaskId) {
        return subtaskIds.remove(subtaskId);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        Epic epic = (Epic) o;
        return Objects.equals(subtaskIds, epic.subtaskIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subtaskIds);
    }

    @Override
    public String toString() {
        return "Epic{" +
                "base=" + super.toString() +
                ", subtaskIds=" + subtaskIds +
                '}';
    }
}
