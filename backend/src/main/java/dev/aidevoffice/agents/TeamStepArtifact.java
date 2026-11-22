package dev.aidevoffice.agents;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TeamStepArtifact(@NotBlank @Size(max=2000) String summary,
        @Valid @Size(max=10) List<AnalysisDraft.DraftTask> tasks,
        @Size(max=12) List<@NotBlank @Size(max=800) String> findings,
        @Size(max=2000) String recommendation) {}
