package dev.aidevoffice.implementation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ImplementationDecisionRequest(
        @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
        @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String artifactHash) {}
