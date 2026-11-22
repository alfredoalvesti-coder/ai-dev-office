package dev.aidevoffice.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aidevoffice.tasks.TaskRequest;
import dev.aidevoffice.tasks.TaskService;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalysisRunService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final TaskService tasks;
    private final ApplicationEventPublisher events;
    private final String promptVersion;
    private final int maxAttempts;
    private final int approvalHours;

    public AnalysisRunService(JdbcTemplate jdbc, ObjectMapper mapper, Validator validator, TaskService tasks,
            ApplicationEventPublisher events,
            @Value("${app.agents.prompt-version:analyst-prompt-v1}") String promptVersion,
            @Value("${app.agents.max-attempts:3}") int maxAttempts,
            @Value("${app.agents.approval-hours:24}") int approvalHours) {
        this.jdbc = jdbc; this.mapper = mapper; this.validator = validator; this.tasks = tasks;
        this.events = events; this.promptVersion = promptVersion; this.maxAttempts = maxAttempts;
        this.approvalHours = approvalHours;
    }

    @Transactional
    public Map<String, Object> create(UUID projectId, AnalysisRunRequest request) {
        if (request.demand().trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Demand must not be blank");
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from office.projects where id = ?)", Boolean.class, projectId)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        var existing = jdbc.query("select * from office.analysis_runs where project_id=? and idempotency_key=?",
                (rs, n) -> row(rs), projectId, request.idempotencyKey().trim());
        if (!existing.isEmpty()) return detail((UUID) existing.get(0).get("id"));
        UUID id = UUID.randomUUID(); Instant now = Instant.now();
        try {
            jdbc.update("insert into office.analysis_runs(id,project_id,demand,idempotency_key,status,context_version,created_at,updated_at) values(?,?,?,?,?,?,?,?)",
                    id, projectId, request.demand().trim(), request.idempotencyKey().trim(), "QUEUED", promptVersion, dbTime(now), dbTime(now));
        } catch (DuplicateKeyException race) { return detailByKey(projectId, request.idempotencyKey()); }
        event(id, null, "QUEUED", "USER", "CREATED");
        events.publishEvent(new QueuedAnalysis(id));
        return detail(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID projectId) {
        return jdbc.query("select id from office.analysis_runs where project_id=? order by created_at desc limit 20",
                (rs,n) -> detail(UUID.fromString(rs.getString(1))), projectId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(UUID id) {
        var found = jdbc.query("select * from office.analysis_runs where id=?", (rs,n) -> row(rs), id);
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis run not found");
        Map<String,Object> run = found.get(0);
        run.put("attempts", jdbc.queryForList("select attempt_number as attemptNumber,status,model,context_version as contextVersion,prompt_tokens as promptTokens,completion_tokens as completionTokens,error_code as errorCode,started_at as startedAt,finished_at as finishedAt from office.analysis_attempts where run_id=? order by attempt_number", id));
        run.put("events", jdbc.queryForList("select from_status as fromStatus,to_status as toStatus,actor,reason,occurred_at as occurredAt from office.analysis_run_events where run_id=? order by occurred_at,id", id));
        run.put("approval", jdbc.query("select decision,artifact_hash as artifactHash,decided_by as decidedBy,created_task_ids as createdTaskIds,decided_at as decidedAt,expires_at as expiresAt from office.analysis_approvals where run_id=?", (rs,n) -> Map.of("decision",rs.getString("decision"),"artifactHash",rs.getString("artifactHash"),"decidedBy",rs.getString("decidedBy"),"createdTaskIds",rs.getString("createdTaskIds"),"decidedAt",rs.getTimestamp("decidedAt").toInstant(),"expiresAt",rs.getTimestamp("expiresAt").toInstant()), id).stream().findFirst().orElse(null));
        return run;
    }

    @Transactional
    public Map<String,Object> retry(UUID id) {
        Map<String,Object> run = basic(id); String status = (String) run.get("status");
        if (status.equals("QUEUED") || status.equals("RUNNING") || status.equals("CANCEL_REQUESTED")) return detail(id);
        if (!status.equals("FAILED")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only failed runs can be retried");
        int count = jdbc.queryForObject("select count(*) from office.analysis_attempts where run_id=?",Integer.class,id);
        if (count >= maxAttempts) throw new ResponseStatusException(HttpStatus.CONFLICT,"Attempt limit reached");
        transition(id,"FAILED","QUEUED","USER","RETRY"); events.publishEvent(new QueuedAnalysis(id)); return detail(id);
    }

    @Transactional
    public Map<String,Object> cancel(UUID id) {
        String status=(String)basic(id).get("status");
        if(status.equals("QUEUED")) transition(id,"QUEUED","CANCELLED","USER","CANCELLED");
        else if(status.equals("RUNNING")) transition(id,"RUNNING","CANCEL_REQUESTED","USER","CANCEL_REQUESTED");
        else if(!status.equals("CANCEL_REQUESTED") && !status.equals("CANCELLED")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Run cannot be cancelled in its current state");
        return detail(id);
    }

    @Transactional
    public Map<String,Object> decide(UUID id, AnalysisDecisionRequest request, String actor) {
        Map<String,Object> run=basic(id); String status=(String)run.get("status");
        if(status.equals("APPROVED") || status.equals("REJECTED")) {
            Map<String,Object> approval=(Map<String,Object>)detail(id).get("approval");
            if(approval.get("decision").equals(request.decision()) && approval.get("artifactHash").toString().equalsIgnoreCase(request.artifactHash())) return detail(id);
            throw new ResponseStatusException(HttpStatus.CONFLICT,"A different decision was already recorded");
        }
        if(!status.equals("AWAITING_APPROVAL")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Run is not awaiting approval");
        Instant expiry=(Instant)run.get("expires_at");
        if(expiry==null || expiry.isBefore(Instant.now())) { transition(id,"AWAITING_APPROVAL","EXPIRED","SYSTEM","APPROVAL_EXPIRED"); return detail(id); }
        String stored=(String)run.get("artifact_hash"), json=(String)run.get("artifact_json");
        if(!request.artifactHash().equalsIgnoreCase(stored) || !sha256(json).equals(stored))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Draft changed; reload before deciding");
        List<UUID> created=List.of();
        if(request.decision().equals("APPROVED")) {
            try {
                AnalysisDraft draft=mapper.readValue(json,AnalysisDraft.class);
                if(!validator.validate(draft).isEmpty()) throw new IllegalArgumentException();
                created=draft.tasks()==null?List.of():draft.tasks().stream().map(t->tasks.create((UUID)run.get("project_id"),new TaskRequest(t.title(),t.description(),t.acceptanceCriteria(),List.of())).id()).toList();
            } catch(Exception ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Stored draft is invalid; no tasks were created"); }
        }
        Instant now=Instant.now(), expires=(Instant)run.get("expires_at");
        jdbc.update("insert into office.analysis_approvals(id,run_id,decision,artifact_hash,decided_by,created_task_ids,decided_at,expires_at) values(?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),id,request.decision(),stored,actor,String.join(",",created.stream().map(UUID::toString).toList()),dbTime(now),dbTime(expires));
        transition(id,"AWAITING_APPROVAL",request.decision(),actor,request.decision()); return detail(id);
    }

    @Transactional
    public boolean start(UUID id, String model) {
        Map<String,Object> run=basic(id); if(!"QUEUED".equals(run.get("status"))) return false;
        transition(id,"QUEUED","RUNNING","SYSTEM","ATTEMPT_STARTED");
        int attempt=jdbc.queryForObject("select count(*) from office.analysis_attempts where run_id=?",Integer.class,id)+1;
        if(attempt>maxAttempts) { transition(id,"RUNNING","FAILED","SYSTEM","ATTEMPT_LIMIT"); return false; }
        jdbc.update("insert into office.analysis_attempts(id,run_id,attempt_number,status,model,context_version,started_at) values(?,?,?,?,?,?,?)",
                UUID.randomUUID(),id,attempt,"RUNNING",model,promptVersion,dbTime(Instant.now())); return true;
    }

    @Transactional(readOnly = true)
    public String demand(UUID id) { return (String)basic(id).get("demand"); }

    @Transactional
    public void complete(UUID id, AnalysisGenerator.GeneratedAnalysis generated) {
        Map<String,Object> run=basic(id);
        if("CANCEL_REQUESTED".equals(run.get("status"))) { transition(id,"CANCEL_REQUESTED","CANCELLED","SYSTEM","CANCELLED"); finishAttempt(id,"CANCELLED","CANCELLED",null,null); return; }
        if(!"RUNNING".equals(run.get("status"))) { finishAttempt(id,"CANCELLED","CANCELLED",null,null); return; }
        var violations=validator.validate(generated.draft());
        if(!violations.isEmpty() || generated.draft().tasks()==null) { fail(id,"OUTPUT_INVALID"); return; }
        try {
            String json=mapper.writeValueAsString(generated.draft()), hash=sha256(json); Instant now=Instant.now(), expiry=now.plus(approvalHours,ChronoUnit.HOURS);
            jdbc.update("update office.analysis_runs set status='AWAITING_APPROVAL',artifact_json=?,artifact_hash=?,model=?,expires_at=?,updated_at=? where id=?",
                    json,hash,generated.model(),dbTime(expiry),dbTime(now),id);
            finishAttempt(id,"SUCCEEDED",null,generated.promptTokens(),generated.completionTokens()); event(id,"RUNNING","AWAITING_APPROVAL","SYSTEM","DRAFT_READY");
        } catch(Exception ex) { fail(id,"PROCESSING_ERROR"); }
    }

    @Transactional
    public void fail(UUID id,String error) {
        String status=(String)basic(id).get("status");
        if(status.equals("CANCEL_REQUESTED")) { transition(id,status,"CANCELLED","SYSTEM","CANCELLED"); finishAttempt(id,"CANCELLED","CANCELLED",null,null); }
        else if(status.equals("RUNNING")) { transition(id,status,"FAILED","SYSTEM",error); finishAttempt(id,"FAILED",error,null,null); }
    }

    private void finishAttempt(UUID id,String status,String error,Integer prompt,Integer completion) {
        jdbc.update("update office.analysis_attempts set status=?,error_code=?,prompt_tokens=?,completion_tokens=?,finished_at=? where run_id=? and status='RUNNING'",status,error,prompt,completion,dbTime(Instant.now()),id);
    }
    private void transition(UUID id,String from,String to,String actor,String reason) {
        int changed=jdbc.update("update office.analysis_runs set status=?,updated_at=? where id=? and status=?",to,dbTime(Instant.now()),id,from);
        if(changed==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Run state changed; reload and retry");
        event(id,from,to,actor,reason);
    }
    private void event(UUID id,String from,String to,String actor,String reason) { jdbc.update("insert into office.analysis_run_events(id,run_id,from_status,to_status,actor,reason,occurred_at) values(?,?,?,?,?,?,?)",UUID.randomUUID(),id,from,to,actor,reason,dbTime(Instant.now())); }
    private java.sql.Timestamp dbTime(Instant instant) { return java.sql.Timestamp.from(instant); }
    private Map<String,Object> basic(UUID id) { var rows=jdbc.query("select * from office.analysis_runs where id=?",(rs,n)->row(rs),id); if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Analysis run not found"); return rows.get(0); }
    private Map<String,Object> detailByKey(UUID project,String key) { var rows=jdbc.query("select id from office.analysis_runs where project_id=? and idempotency_key=?",(rs,n)->UUID.fromString(rs.getString(1)),project,key); return detail(rows.get(0)); }
    private Map<String,Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String,Object> result=new java.util.LinkedHashMap<>();
        result.put("id",rs.getObject("id",UUID.class)); result.put("project_id",rs.getObject("project_id",UUID.class));
        result.put("demand",rs.getString("demand")); result.put("idempotency_key",rs.getString("idempotency_key"));
        result.put("status",rs.getString("status")); result.put("artifact_json",rs.getString("artifact_json"));
        result.put("artifact_hash",rs.getString("artifact_hash")); result.put("model",rs.getString("model"));
        result.put("context_version",rs.getString("context_version"));
        java.sql.Timestamp expiry=rs.getTimestamp("expires_at"); result.put("expires_at",expiry==null?null:expiry.toInstant());
        result.put("error_code",rs.getString("error_code")); result.put("created_at",rs.getTimestamp("created_at").toInstant());
        result.put("updated_at",rs.getTimestamp("updated_at").toInstant()); return result;
    }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception ex) { throw new IllegalStateException(ex); } }
    public record QueuedAnalysis(UUID id) {}
}
