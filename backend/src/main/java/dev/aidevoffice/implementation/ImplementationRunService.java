package dev.aidevoffice.implementation;

import tools.jackson.databind.ObjectMapper;
import dev.aidevoffice.agents.TeamRunService;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImplementationRunService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final WorkspaceApplier workspace;
    private final ApplicationEventPublisher publisher;
    private final int approvalHours;

    public ImplementationRunService(JdbcTemplate jdbc, ObjectMapper mapper, Validator validator,
            WorkspaceApplier workspace, ApplicationEventPublisher publisher,
            @Value("${app.agents.approval-hours:24}") int approvalHours) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.validator = validator;
        this.workspace = workspace;
        this.publisher = publisher;
        this.approvalHours = approvalHours;
    }

    public record ImplementationQueued(UUID id) {}

    @Transactional
    public Map<String, Object> create(UUID projectId, UUID teamRunId, String actor) {
        Map<String, Object> team = teamRun(projectId, teamRunId);
        if (!"APPROVED".equals(team.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only approved team plans can be implemented");
        }
        List<UUID> active = jdbc.query("""
                select id from office.implementation_runs
                where team_run_id=? and status in ('QUEUED','RUNNING','AWAITING_APPROVAL')
                """, (rs, n) -> rs.getObject(1, UUID.class), teamRunId);
        if (!active.isEmpty()) {
            return detail(active.getFirst());
        }
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                insert into office.implementation_runs
                (id,project_id,team_run_id,status,requested_by,created_at,updated_at)
                values (?,?,?,'QUEUED',?,?,?)
                """, id, projectId, teamRunId, actor, time(now), time(now));
        event(id, projectId, "IMPLEMENTATION_QUEUED", Map.of("teamRunId", teamRunId.toString()));
        publisher.publishEvent(new ImplementationQueued(id));
        return detail(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID projectId) {
        requireProject(projectId);
        return jdbc.query("select id from office.implementation_runs where project_id=? order by created_at desc limit 20",
                (rs, n) -> detail(rs.getObject(1, UUID.class)), projectId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(UUID id) {
        Map<String, Object> run = basic(id);
        run.put("workspaceFiles", workspace.list((UUID) run.get("project_id")));
        return run;
    }

    @Transactional
    public boolean start(UUID id) {
        Map<String, Object> run = basic(id);
        if (!"QUEUED".equals(run.get("status"))) {
            return false;
        }
        int changed = jdbc.update(
                "update office.implementation_runs set status='RUNNING',updated_at=? where id=? and status='QUEUED'",
                time(Instant.now()), id);
        if (changed == 0) {
            return false;
        }
        event(id, (UUID) run.get("project_id"), "IMPLEMENTATION_STARTED", Map.of("role", "DEVELOPER"));
        return true;
    }

    @Transactional(readOnly = true)
    public GenerationContext context(UUID id) {
        Map<String, Object> run = basic(id);
        Map<String, Object> team = teamRun((UUID) run.get("project_id"), (UUID) run.get("team_run_id"));
        return new GenerationContext((UUID) run.get("project_id"), (UUID) run.get("team_run_id"),
                (String) team.get("demand"), (String) team.get("artifact_json"));
    }

    public record GenerationContext(UUID projectId, UUID teamRunId, String demand, String planJson) {}

    @Transactional
    public void complete(UUID id, ImplementationProposal proposal, String model) {
        Map<String, Object> run = basic(id);
        if (!"RUNNING".equals(run.get("status"))) {
            return;
        }
        if (!validator.validate(proposal).isEmpty()) {
            fail(id, "OUTPUT_INVALID");
            return;
        }
        try {
            workspace.validateProposal(proposal);
        } catch (RuntimeException ex) {
            fail(id, "OUTPUT_INVALID");
            return;
        }
        String json = write(proposal);
        String hash = hash(json);
        Instant now = Instant.now();
        Instant expiry = now.plus(approvalHours, ChronoUnit.HOURS);
        jdbc.update("""
                update office.implementation_runs
                set status='AWAITING_APPROVAL',artifact_json=?,artifact_hash=?,model=?,expires_at=?,updated_at=?
                where id=? and status='RUNNING'
                """, json, hash, model, time(expiry), time(now), id);
        event(id, (UUID) run.get("project_id"), "IMPLEMENTATION_READY",
                Map.of("artifactHash", hash, "fileCount", proposal.files().size(), "role", "DEVELOPER"));
    }

    @Transactional
    public void fail(UUID id, String code) {
        Map<String, Object> run = basic(id);
        String status = (String) run.get("status");
        if (!status.equals("QUEUED") && !status.equals("RUNNING")) {
            return;
        }
        jdbc.update("update office.implementation_runs set status='FAILED',error_code=?,updated_at=? where id=?",
                code, time(Instant.now()), id);
        event(id, (UUID) run.get("project_id"), "IMPLEMENTATION_FAILED", Map.of("errorCode", code));
    }

    @Transactional
    public Map<String, Object> decide(UUID id, ImplementationDecisionRequest request, String actor) {
        Map<String, Object> run = basic(id);
        String status = (String) run.get("status");
        if (status.equals("APPLIED") || status.equals("REJECTED")) {
            if (request.decision().equals(status.equals("APPLIED") ? "APPROVED" : "REJECTED")
                    && request.artifactHash().equalsIgnoreCase(String.valueOf(run.get("artifact_hash")))) {
                return detail(id);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A different implementation decision was recorded");
        }
        if (!status.equals("AWAITING_APPROVAL")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Implementation is not awaiting approval");
        }
        Instant expiry = (Instant) run.get("expires_at");
        if (expiry == null || expiry.isBefore(Instant.now())) {
            jdbc.update("update office.implementation_runs set status='EXPIRED',updated_at=? where id=?",
                    time(Instant.now()), id);
            event(id, (UUID) run.get("project_id"), "IMPLEMENTATION_EXPIRED", Map.of());
            return detail(id);
        }
        String json = (String) run.get("artifact_json");
        String stored = (String) run.get("artifact_hash");
        if (!request.artifactHash().equalsIgnoreCase(stored) || !hash(json).equals(stored)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Proposal changed; reload before deciding");
        }
        Instant now = Instant.now();
        if (request.decision().equals("REJECTED")) {
            jdbc.update("""
                    update office.implementation_runs
                    set status='REJECTED',approved_by=?,updated_at=? where id=?
                    """, actor, time(now), id);
            event(id, (UUID) run.get("project_id"), "IMPLEMENTATION_REJECTED", Map.of());
            return detail(id);
        }
        ImplementationProposal proposal;
        try {
            proposal = mapper.readValue(json, ImplementationProposal.class);
            if (!validator.validate(proposal).isEmpty()) {
                throw new IllegalArgumentException();
            }
            workspace.validateProposal(proposal);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approved proposal is invalid");
        }
        UUID projectId = (UUID) run.get("project_id");
        List<String> written = workspace.apply(projectId, proposal);
        String relative = workspace.relativeWorkspace(projectId);
        jdbc.update("""
                update office.implementation_runs
                set status='APPLIED',approved_by=?,applied_at=?,workspace_relative=?,updated_at=?
                where id=? and status='AWAITING_APPROVAL'
                """, actor, time(now), relative, time(now), id);
        event(id, projectId, "IMPLEMENTATION_APPLIED", Map.of("files", written, "workspace", relative));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> cancel(UUID id) {
        Map<String, Object> run = basic(id);
        String status = (String) run.get("status");
        if (!status.equals("QUEUED") && !status.equals("RUNNING") && !status.equals("AWAITING_APPROVAL")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Implementation cannot be cancelled");
        }
        jdbc.update("update office.implementation_runs set status='CANCELLED',updated_at=? where id=?",
                time(Instant.now()), id);
        event(id, (UUID) run.get("project_id"), "IMPLEMENTATION_CANCELLED", Map.of("fromStatus", status));
        return detail(id);
    }

    @Transactional
    public void recoverInterrupted() {
        List<UUID> interrupted = jdbc.query(
                "select id from office.implementation_runs where status in ('QUEUED','RUNNING')",
                (rs, n) -> rs.getObject(1, UUID.class));
        for (UUID id : interrupted) {
            if ("QUEUED".equals(basic(id).get("status"))) {
                publisher.publishEvent(new ImplementationQueued(id));
            } else {
                fail(id, "INTERRUPTED");
            }
        }
    }

    private Map<String, Object> teamRun(UUID projectId, UUID teamRunId) {
        List<Map<String, Object>> rows = jdbc.query("""
                select id, project_id, demand, status, artifact_json, artifact_hash
                from office.team_runs where id=? and project_id=?
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getObject("id", UUID.class));
            row.put("project_id", rs.getObject("project_id", UUID.class));
            row.put("demand", rs.getString("demand"));
            row.put("status", rs.getString("status"));
            row.put("artifact_json", rs.getString("artifact_json"));
            row.put("artifact_hash", rs.getString("artifact_hash"));
            return row;
        }, teamRunId, projectId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team run not found");
        }
        return rows.getFirst();
    }

    private void requireProject(UUID projectId) {
        Boolean exists = jdbc.queryForObject("select exists(select 1 from office.projects where id=?)", Boolean.class, projectId);
        if (!Boolean.TRUE.equals(exists)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }

    private Map<String, Object> basic(UUID id) {
        List<Map<String, Object>> rows = jdbc.query("select * from office.implementation_runs where id=?", (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getObject("id", UUID.class));
            row.put("project_id", rs.getObject("project_id", UUID.class));
            row.put("team_run_id", rs.getObject("team_run_id", UUID.class));
            row.put("status", rs.getString("status"));
            row.put("artifact_json", rs.getString("artifact_json"));
            row.put("artifact_hash", rs.getString("artifact_hash"));
            row.put("model", rs.getString("model"));
            row.put("error_code", rs.getString("error_code"));
            var expiry = rs.getTimestamp("expires_at");
            row.put("expires_at", expiry == null ? null : expiry.toInstant());
            row.put("workspace_relative", rs.getString("workspace_relative"));
            row.put("requested_by", rs.getString("requested_by"));
            row.put("approved_by", rs.getString("approved_by"));
            var applied = rs.getTimestamp("applied_at");
            row.put("applied_at", applied == null ? null : applied.toInstant());
            row.put("created_at", rs.getTimestamp("created_at").toInstant());
            row.put("updated_at", rs.getTimestamp("updated_at").toInstant());
            return row;
        }, id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Implementation run not found");
        }
        return rows.getFirst();
    }

    private void event(UUID executionId, UUID projectId, String type, Map<String, Object> payload) {
        try {
            Map<String, Object> body = new LinkedHashMap<>(payload);
            UUID eventId = UUID.randomUUID();
            Instant occurred = Instant.now();
            String json = mapper.writeValueAsString(body);
            jdbc.update("""
                    insert into office.project_events(event_id,project_id,execution_id,event_type,payload_json,occurred_at)
                    values (?,?,?,?,?,?)
                    """, eventId, projectId, executionId, type, json, time(occurred));
            Long cursor = jdbc.queryForObject("select cursor from office.project_events where event_id=?", Long.class, eventId);
            publisher.publishEvent(new TeamRunService.ProjectEvent(cursor, eventId, projectId, executionId, type, body, occurred));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not persist implementation event", ex);
        }
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private java.sql.Timestamp time(Instant instant) {
        return java.sql.Timestamp.from(instant);
    }
}
