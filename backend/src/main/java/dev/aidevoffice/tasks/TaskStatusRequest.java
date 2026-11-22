package dev.aidevoffice.tasks;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TaskStatusRequest(@NotNull TaskStatus status, @NotNull @PositiveOrZero Long version) {}
