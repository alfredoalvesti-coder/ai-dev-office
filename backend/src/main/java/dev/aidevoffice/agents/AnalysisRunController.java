package dev.aidevoffice.agents;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalysisRunController {
    private final AnalysisRunService runs;
    public AnalysisRunController(AnalysisRunService runs) { this.runs=runs; }

    @PostMapping("/api/projects/{projectId}/analysis-runs")
    public Map<String,Object> create(@PathVariable UUID projectId,@Valid @RequestBody AnalysisRunRequest request) { return runs.create(projectId,request); }
    @GetMapping("/api/projects/{projectId}/analysis-runs")
    public List<Map<String,Object>> list(@PathVariable UUID projectId) { return runs.list(projectId); }
    @GetMapping("/api/analysis-runs/{id}")
    public Map<String,Object> detail(@PathVariable UUID id) { return runs.detail(id); }
    @PostMapping("/api/analysis-runs/{id}/retry")
    public Map<String,Object> retry(@PathVariable UUID id) { return runs.retry(id); }
    @PostMapping("/api/analysis-runs/{id}/cancel")
    public Map<String,Object> cancel(@PathVariable UUID id) { return runs.cancel(id); }
    @PostMapping("/api/analysis-runs/{id}/decision")
    public Map<String,Object> decide(@PathVariable UUID id,@Valid @RequestBody AnalysisDecisionRequest request,Authentication auth) { return runs.decide(id,request,auth.getName()); }
}
