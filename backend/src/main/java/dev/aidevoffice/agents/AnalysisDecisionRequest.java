package dev.aidevoffice.agents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AnalysisDecisionRequest(@NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
                                     @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String artifactHash) {}
