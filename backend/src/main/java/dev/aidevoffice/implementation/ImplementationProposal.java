package dev.aidevoffice.implementation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ImplementationProposal(
        @NotBlank @Size(max = 2000) String summary,
        @Valid @Size(min = 1, max = 8) List<ProposedFile> files) {}
