package dev.aidevoffice.tasks;

import java.time.Instant;
import java.util.UUID;

public record TaskHistoryResponse(UUID id, TaskStatus fromStatus, TaskStatus toStatus, Instant changedAt) {
    static TaskHistoryResponse from(TaskStatusHistory history) {
        return new TaskHistoryResponse(history.getId(), history.getFromStatus(), history.getToStatus(), history.getChangedAt());
    }
}
