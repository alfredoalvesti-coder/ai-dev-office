package dev.aidevoffice.tasks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record TaskRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 4000) String description,
        @Size(max = 30) List<@NotBlank @Size(max = 500) String> acceptanceCriteria,
        @Size(max = 30) List<UUID> dependencyIds) {}
