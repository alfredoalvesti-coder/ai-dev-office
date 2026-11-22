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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamRunController {
    private final TeamRunService runs;
    private final WebSocketTicketService tickets;
    public TeamRunController(TeamRunService runs,WebSocketTicketService tickets) { this.runs=runs;this.tickets=tickets; }

    @PostMapping("/api/projects/{projectId}/team-runs")
    public Map<String,Object> create(@PathVariable UUID projectId,@Valid @RequestBody TeamRunRequest request) { return runs.create(projectId,request); }
    @GetMapping("/api/projects/{projectId}/team-runs")
    public List<Map<String,Object>> list(@PathVariable UUID projectId) { return runs.list(projectId); }
    @GetMapping("/api/team-runs/{id}")
    public Map<String,Object> detail(@PathVariable UUID id) { return runs.detail(id); }
    @PostMapping("/api/team-runs/{id}/retry")
    public Map<String,Object> retry(@PathVariable UUID id) { return runs.retry(id); }
    @PostMapping("/api/team-runs/{id}/cancel")
    public Map<String,Object> cancel(@PathVariable UUID id) { return runs.cancel(id); }
    @PostMapping("/api/team-runs/{id}/decision")
    public Map<String,Object> decision(@PathVariable UUID id,@Valid @RequestBody TeamDecisionRequest request,Authentication auth) { return runs.decide(id,request,auth.getName()); }
    @GetMapping("/api/projects/{projectId}/events")
    public List<Map<String,Object>> history(@PathVariable UUID projectId,@RequestParam(defaultValue="0") long after,@RequestParam(defaultValue="200") int limit) { return runs.history(projectId,after,limit); }
    @PostMapping("/api/projects/{projectId}/events/tickets")
    public Map<String,String> ticket(@PathVariable UUID projectId,Authentication auth) {
        if(!runs.projectExists(projectId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Project not found");
        return Map.of("ticket",tickets.issue(projectId,auth.getName()),"protocol","ai-office-ticket");
    }
}
