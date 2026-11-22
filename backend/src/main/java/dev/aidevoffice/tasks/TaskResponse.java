package dev.aidevoffice.tasks;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(UUID id, UUID projectId, String title, String description,
                           List<String> acceptanceCriteria, List<UUID> dependencyIds,
                           TaskStatus status, Instant createdAt, Instant updatedAt, long version) {
    static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProject().getId(), task.getTitle(), task.getDescription(),
                task.getAcceptanceCriteria(), task.getDependencies().stream().map(Task::getId).sorted().toList(),
                task.getStatus(), task.getCreatedAt(), task.getUpdatedAt(), task.getVersion());
    }
}
