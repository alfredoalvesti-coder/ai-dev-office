package dev.aidevoffice.projects;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(UUID id, String name, String description,
                              Instant createdAt, Instant updatedAt, long version) {
    static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                project.getCreatedAt(), project.getUpdatedAt(), project.getVersion());
    }
}
