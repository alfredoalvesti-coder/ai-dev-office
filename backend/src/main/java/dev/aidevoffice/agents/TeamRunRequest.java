package dev.aidevoffice.agents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamRunRequest(@NotBlank @Size(max=5000) String demand,@NotBlank @Size(max=100) String idempotencyKey) {}
