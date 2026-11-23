package dev.aidevoffice.implementation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProposedFile(
        @NotBlank @Size(max = 200) String path,
        @NotBlank @Size(max = 16384) String content,
        @Size(max = 500) String rationale) {}
