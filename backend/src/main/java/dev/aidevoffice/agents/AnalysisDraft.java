package dev.aidevoffice.agents;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AnalysisDraft(@NotBlank @Size(max = 2000) String summary,
                            @Size(max = 10) List<@Valid DraftTask> tasks) {
    public record DraftTask(@NotBlank @Size(max = 180) String title,
                            @Size(max = 4000) String description,
                            @Size(max = 30) List<@NotBlank @Size(max = 500) String> acceptanceCriteria) {}
}
