package dev.aidevoffice.implementation;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImplementationRunController {
    private final ImplementationRunService runs;
    private final WorkspaceApplier workspace;

    public ImplementationRunController(ImplementationRunService runs, WorkspaceApplier workspace) {
        this.runs = runs;
        this.workspace = workspace;
    }

    @PostMapping("/api/projects/{projectId}/team-runs/{teamRunId}/implementations")
    public Map<String, Object> create(@PathVariable UUID projectId, @PathVariable UUID teamRunId, Authentication auth) {
        return runs.create(projectId, teamRunId, auth.getName());
    }

    @GetMapping("/api/projects/{projectId}/implementations")
    public List<Map<String, Object>> list(@PathVariable UUID projectId) {
        return runs.list(projectId);
    }

    @GetMapping("/api/implementations/{id}")
    public Map<String, Object> detail(@PathVariable UUID id) {
        return runs.detail(id);
    }

    @PostMapping("/api/implementations/{id}/decision")
    public Map<String, Object> decision(@PathVariable UUID id, @Valid @RequestBody ImplementationDecisionRequest request,
            Authentication auth) {
        return runs.decide(id, request, auth.getName());
    }

    @PostMapping("/api/implementations/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable UUID id) {
        return runs.cancel(id);
    }

    @GetMapping("/api/projects/{projectId}/workspace/files")
    public List<String> workspaceFiles(@PathVariable UUID projectId) {
        return workspace.list(projectId);
    }

    @GetMapping("/api/projects/{projectId}/workspace/files/content")
    public Map<String, String> workspaceFile(@PathVariable UUID projectId, @RequestParam String path) {
        return Map.of("path", path, "content", workspace.read(projectId, path));
    }
}
