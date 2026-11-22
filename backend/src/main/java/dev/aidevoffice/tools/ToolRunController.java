package dev.aidevoffice.tools;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/tool-runs")
class ToolRunController {
    private static final String MUTATION_HEADER = "X-Office-Tool-Request=1";
    private final ToolRunService service;

    ToolRunController(ToolRunService service) {
        this.service = service;
    }

    @GetMapping
    History list(@PathVariable UUID projectId) {
        return new History(service.enabled(), service.list(projectId));
    }

    @PostMapping(headers = MUTATION_HEADER)
    @ResponseStatus(HttpStatus.CREATED)
    ToolRunService.Run create(@PathVariable UUID projectId, @Valid @RequestBody Proposal body, Principal actor) {
        return service.create(projectId, body.tool(), body.source(), actor.getName());
    }

    @GetMapping("/{id}")
    ToolRunService.Run get(@PathVariable UUID projectId, @PathVariable UUID id) {
        return service.get(projectId, id);
    }

    @PostMapping(value = "/{id}/approve", headers = MUTATION_HEADER)
    ToolRunService.Run approve(@PathVariable UUID projectId, @PathVariable UUID id,
                               @Valid @RequestBody Decision body, Principal actor) {
        return service.approve(projectId, id, body.hash(), actor.getName());
    }

    @PostMapping(value = "/{id}/cancel", headers = MUTATION_HEADER)
    ToolRunService.Run cancel(@PathVariable UUID projectId, @PathVariable UUID id) {
        return service.cancel(projectId, id);
    }

    record Proposal(@NotNull ToolRunService.Tool tool, @NotBlank @Size(max = 8192) String source) {}
    record Decision(@NotNull @Pattern(regexp = "[a-f0-9]{64}") String hash) {}
    record History(boolean enabled, List<ToolRunService.Run> runs) {}
}
