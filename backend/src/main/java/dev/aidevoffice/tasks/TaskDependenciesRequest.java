package dev.aidevoffice.tasks;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record TaskDependenciesRequest(@NotNull @Size(max = 30) List<@NotNull UUID> dependencyIds,
                                      @NotNull @PositiveOrZero Long version) {}
