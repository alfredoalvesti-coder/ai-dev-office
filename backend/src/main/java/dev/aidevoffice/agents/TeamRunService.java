package dev.aidevoffice.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aidevoffice.tasks.TaskRequest;
import dev.aidevoffice.tasks.TaskService;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
public class TeamRunService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final TaskService tasks;
    private final ApplicationEventPublisher publisher;
    private final int approvalHours;
    private final int maxAttempts;
    private final String contextVersion;

    public TeamRunService(JdbcTemplate jdbc,ObjectMapper mapper,Validator validator,TaskService tasks,ApplicationEventPublisher publisher,
            @Value("${app.agents.approval-hours:24}") int approvalHours,
            @Value("${app.team.max-attempts:3}") int maxAttempts,
            @Value("${app.team.context-version:team-workflow-v1}") String contextVersion) {
        this.jdbc=jdbc; this.mapper=mapper; this.validator=validator; this.tasks=tasks; this.publisher=publisher;
        this.approvalHours=approvalHours; this.maxAttempts=maxAttempts; this.contextVersion=contextVersion;
    }

    @Transactional
    public Map<String,Object> create(UUID projectId,TeamRunRequest request) {
        if(request.demand().trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Demand must not be blank");
        if(jdbc.query("select id from office.projects where id=? for update",(rs,n)->rs.getObject(1,UUID.class),projectId).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found");
        List<UUID> found=jdbc.query("select id from office.team_runs where project_id=? and idempotency_key=?",(rs,n)->rs.getObject(1,UUID.class),projectId,request.idempotencyKey().trim());
        if(!found.isEmpty()) return detail(found.getFirst());
        UUID id=UUID.randomUUID(); Instant now=Instant.now();
        jdbc.update("insert into office.team_runs(id,project_id,demand,idempotency_key,status,attempt_count,context_version,created_at,updated_at) values(?,?,?,?,?,?,?,?,?)",
                id,projectId,request.demand().trim(),request.idempotencyKey().trim(),"QUEUED",0,contextVersion,time(now),time(now));
        transitionEvent(id,projectId,null,"QUEUED","RUN_CREATED",Map.of());
        publisher.publishEvent(new TeamQueued(id));
        return detail(id);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> list(UUID projectId) {
        return jdbc.query("select id from office.team_runs where project_id=? order by created_at desc limit 20",(rs,n)->detail(rs.getObject(1,UUID.class)),projectId);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> detail(UUID id) {
        Map<String,Object> run=basic(id);
        run.put("steps",jdbc.queryForList("select attempt_number as attemptNumber,step_number as stepNumber,role,status,artifact_json as artifactJson,error_code as errorCode,started_at as startedAt,finished_at as finishedAt from office.team_steps where run_id=? order by attempt_number,step_number",id));
        run.put("approval",jdbc.query("select decision,artifact_hash as artifactHash,decided_by as decidedBy,created_task_ids as createdTaskIds,decided_at as decidedAt,expires_at as expiresAt from office.team_approvals where run_id=?",(rs,n)->Map.of("decision",rs.getString(1),"artifactHash",rs.getString(2),"decidedBy",rs.getString(3),"createdTaskIds",rs.getString(4),"decidedAt",rs.getTimestamp(5).toInstant(),"expiresAt",rs.getTimestamp(6).toInstant()),id).stream().findFirst().orElse(null));
        return run;
    }

    @Transactional
    public boolean start(UUID id) {
        Map<String,Object> run=basic(id); if(!"QUEUED".equals(run.get("status"))) return false;
        int attempt=(Integer)run.get("attempt_count")+1;
        int changed=jdbc.update("update office.team_runs set status='RUNNING',attempt_count=?,updated_at=? where id=? and status='QUEUED'",attempt,time(Instant.now()),id);
        if(changed==0) return false;
        transitionEvent(id,(UUID)run.get("project_id"),"QUEUED","RUNNING","RUN_STARTED",Map.of("attempt",attempt)); return true;
    }

    @Transactional(readOnly=true)
    public TeamRunContext context(UUID id) {
        Map<String,Object> run=basic(id); int attempt=(Integer)run.get("attempt_count");
        List<Map<String,Object>> steps=jdbc.query("select role,artifact_json from office.team_steps where run_id=? and attempt_number=? and status='SUCCEEDED' order by step_number",(rs,n)->Map.of("role",rs.getString(1),"artifact",rs.getString(2)),id,attempt);
        return new TeamRunContext((UUID)run.get("project_id"),(String)run.get("demand"),attempt,(String)run.get("model"),steps);
    }

    @Transactional
    public boolean beginStep(UUID id,int attempt,int number,TeamRole role) {
        Map<String,Object> run=basic(id); if(!"RUNNING".equals(run.get("status"))) return false;
        jdbc.update("insert into office.team_steps(id,run_id,attempt_number,step_number,role,status,started_at) values(?,?,?,?,?,?,?)",UUID.randomUUID(),id,attempt,number,role.name(),"RUNNING",time(Instant.now()));
        transitionEvent(id,(UUID)run.get("project_id"),"RUNNING","RUNNING","STEP_STARTED",Map.of("role",role.name(),"step",number)); return true;
    }

    @Transactional
    public void stepSucceeded(UUID id,int attempt,int number,TeamRole role,TeamStepArtifact artifact) {
        if(!validator.validate(artifact).isEmpty() || artifact.tasks()==null) throw new IllegalArgumentException("OUTPUT_INVALID");
        String json=write(artifact); Map<String,Object> run=basic(id);
        if("CANCEL_REQUESTED".equals(run.get("status"))) { jdbc.update("update office.team_steps set status='CANCELLED',finished_at=? where run_id=? and attempt_number=? and step_number=? and status='RUNNING'",time(Instant.now()),id,attempt,number); markRemaining(id,attempt,number,"CANCELLED"); setState(id,run,"CANCELLED","CANCELLED",Map.of("role",role.name(),"step",number)); return; }
        if(!"RUNNING".equals(run.get("status"))) return;
        jdbc.update("update office.team_steps set status='SUCCEEDED',artifact_json=?,finished_at=? where run_id=? and attempt_number=? and step_number=? and status='RUNNING'",json,time(Instant.now()),id,attempt,number);
        transitionEvent(id,(UUID)run.get("project_id"),"RUNNING","RUNNING","STEP_COMPLETED",Map.of("role",role.name(),"step",number,"summary",artifact.summary()));
    }

    @Transactional
    public void complete(UUID id,int attempt,List<TeamStepArtifact> artifacts,String model) {
        Map<String,Object> run=basic(id);
        if("CANCEL_REQUESTED".equals(run.get("status"))) { setState(id,run,"CANCELLED","CANCELLED",Map.of()); return; }
        if(!"RUNNING".equals(run.get("status"))) return;
        try {
            List<TeamRoleResult> results=new ArrayList<>(); TeamRole[] roles=TeamRole.values();
            for(int i=0;i<roles.length;i++) results.add(new TeamRoleResult(roles[i].name(),artifacts.get(i)));
            List<AnalysisDraft.DraftTask> draftTasks=artifacts.get(1).tasks()==null?List.of():artifacts.get(1).tasks();
            TeamFinalArtifact finalArtifact=new TeamFinalArtifact(artifacts.get(0).summary(),draftTasks,results);
            if(!validator.validate(finalArtifact).isEmpty()) { fail(id,"OUTPUT_INVALID"); return; }
            String json=write(finalArtifact),hash=hash(json); Instant now=Instant.now(),expiry=now.plus(approvalHours,ChronoUnit.HOURS);
            jdbc.update("update office.team_runs set status='AWAITING_APPROVAL',artifact_json=?,artifact_hash=?,model=?,expires_at=?,updated_at=? where id=?",json,hash,model,time(expiry),time(now),id);
            transitionEvent(id,(UUID)run.get("project_id"),"RUNNING","AWAITING_APPROVAL","TEAM_REVIEW_READY",Map.of("artifactHash",hash,"expiresAt",expiry));
        } catch(Exception ex) { fail(id,"PROCESSING_ERROR"); }
    }

    @Transactional
    public void fail(UUID id,int attempt,int step,String code) {
        Map<String,Object> run=basic(id); if(!"RUNNING".equals(run.get("status")) && !"CANCEL_REQUESTED".equals(run.get("status"))) return;
        String outcome="CANCEL_REQUESTED".equals(run.get("status"))?"CANCELLED":"FAILED";
        jdbc.update("update office.team_steps set status=?,error_code=?,finished_at=? where run_id=? and attempt_number=? and step_number=? and status='RUNNING'",outcome.equals("FAILED")?"FAILED":"CANCELLED",code,time(Instant.now()),id,attempt,step);
        markRemaining(id,attempt,step,outcome.equals("FAILED")?"SKIPPED":"CANCELLED");
        setState(id,run,outcome,code,Map.of("role",step>=1&&step<=TeamRole.values().length?TeamRole.values()[step-1].name():"UNKNOWN","step",step));
    }

    @Transactional
    public Map<String,Object> retry(UUID id) {
        Map<String,Object> run=basic(id); if("QUEUED".equals(run.get("status"))||"RUNNING".equals(run.get("status"))) return detail(id);
        if(!"FAILED".equals(run.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only failed team runs can be retried");
        if((Integer)run.get("attempt_count")>=maxAttempts) throw new ResponseStatusException(HttpStatus.CONFLICT,"Team attempt limit reached");
        setState(id,run,"QUEUED","RETRY",Map.of()); publisher.publishEvent(new TeamQueued(id)); return detail(id);
    }

    @Transactional
    public Map<String,Object> cancel(UUID id) {
        Map<String,Object> run=basic(id); String state=(String)run.get("status");
        if(state.equals("QUEUED")) setState(id,run,"CANCELLED","CANCELLED",Map.of());
        else if(state.equals("RUNNING")) setState(id,run,"CANCEL_REQUESTED","CANCEL_REQUESTED",Map.of());
        else if(!state.equals("CANCEL_REQUESTED")&&!state.equals("CANCELLED")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Team run cannot be cancelled");
        return detail(id);
    }

    @Transactional
    public Map<String,Object> decide(UUID id,TeamDecisionRequest request,String actor) {
        Map<String,Object> run=basic(id); String state=(String)run.get("status");
        if(state.equals("APPROVED")||state.equals("REJECTED")) {
            Map<String,Object> approval=detail(id).get("approval") instanceof Map<?,?> row?(Map<String,Object>)row:null;
            if(approval!=null&&request.decision().equals(approval.get("decision"))&&request.artifactHash().equalsIgnoreCase(String.valueOf(approval.get("artifactHash")))) return detail(id);
            throw new ResponseStatusException(HttpStatus.CONFLICT,"A different team decision was already recorded");
        }
        if(!state.equals("AWAITING_APPROVAL")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Team run is not awaiting approval");
        Instant expiry=(Instant)run.get("expires_at");
        if(expiry==null||expiry.isBefore(Instant.now())) { setState(id,run,"EXPIRED","APPROVAL_EXPIRED",Map.of()); return detail(id); }
        String json=(String)run.get("artifact_json"),stored=(String)run.get("artifact_hash");
        if(!request.artifactHash().equalsIgnoreCase(stored)||!hash(json).equals(stored)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Plan changed; reload before deciding");
        List<UUID> created=List.of();
        if(request.decision().equals("APPROVED")) {
            try { TeamFinalArtifact artifact=mapper.readValue(json,TeamFinalArtifact.class);
                if(!validator.validate(artifact).isEmpty()) throw new IllegalArgumentException();
                created=artifact.tasks().stream().map(task->tasks.create((UUID)run.get("project_id"),new TaskRequest(task.title(),task.description(),task.acceptanceCriteria(),List.of())).id()).toList();
            } catch(Exception ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Approved plan is invalid; no tasks were created"); }
        }
        Instant now=Instant.now(); jdbc.update("insert into office.team_approvals(id,run_id,decision,artifact_hash,decided_by,created_task_ids,decided_at,expires_at) values(?,?,?,?,?,?,?,?)",UUID.randomUUID(),id,request.decision(),stored,actor,String.join(",",created.stream().map(UUID::toString).toList()),time(now),time(expiry));
        setState(id,run,request.decision(),request.decision(),Map.of("createdTaskCount",created.size())); return detail(id);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> history(UUID projectId,long after,int limit) {
        if(!projectExists(projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found");
        if(limit<1||limit>500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"limit must be between 1 and 500");
        return jdbc.query("select cursor,event_id as eventId,project_id as projectId,execution_id as executionId,event_type as type,payload_json as payload,occurred_at as occurredAt from office.project_events where project_id=? and cursor>? order by cursor limit ?",(rs,n)->{
            Map<String,Object> item=new LinkedHashMap<>(); item.put("cursor",rs.getLong("cursor")); item.put("eventId",rs.getObject("eventId",UUID.class)); item.put("projectId",rs.getObject("projectId",UUID.class)); item.put("executionId",rs.getObject("executionId",UUID.class)); item.put("type",rs.getString("type")); item.put("payload",parseJson(rs.getString("payload"))); item.put("occurredAt",rs.getTimestamp("occurredAt").toInstant()); return item;
        },projectId,after,limit);
    }

    public record TeamQueued(UUID id) {}
    public record ProjectEvent(long cursor,UUID eventId,UUID projectId,UUID executionId,String type,Map<String,Object> payload,Instant occurredAt) {}
    public record TeamRunContext(UUID projectId,String demand,int attempt,String model,List<Map<String,Object>> priorSteps) {}
    public record TeamRoleResult(String role,TeamStepArtifact artifact) {}
    public record TeamFinalArtifact(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=2000) String summary,
            @jakarta.validation.Valid @jakarta.validation.constraints.Size(max=10) List<AnalysisDraft.DraftTask> tasks,
            @jakarta.validation.constraints.Size(min=6,max=6) List<TeamRoleResult> steps) {}

    @Transactional(readOnly=true)
    public boolean projectExists(UUID projectId) { return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from office.projects where id=?)",Boolean.class,projectId)); }

    @Transactional
    public void recoverInterrupted() {
        List<UUID> interrupted=jdbc.query("select id from office.team_runs where status='RUNNING'",(rs,n)->rs.getObject(1,UUID.class));
        for(UUID id:interrupted) {
            int attempt=jdbc.queryForObject("select attempt_count from office.team_runs where id=?",Integer.class,id);
            Integer active=jdbc.query("select step_number from office.team_steps where run_id=? and attempt_number=? and status='RUNNING'",(rs,n)->rs.getInt(1),id,attempt).stream().findFirst().orElse(0);
            jdbc.update("update office.team_steps set status='FAILED',error_code='INTERRUPTED',finished_at=? where run_id=? and status='RUNNING'",time(Instant.now()),id);
            markRemaining(id,attempt,active,"SKIPPED");
            Map<String,Object> run=basic(id); setState(id,run,"FAILED","INTERRUPTED",Map.of());
        }
        List<UUID> queued=jdbc.query("select id from office.team_runs where status='QUEUED'",(rs,n)->rs.getObject(1,UUID.class));
        queued.forEach(id->publisher.publishEvent(new TeamQueued(id)));
    }

    private void fail(UUID id,String code) { Map<String,Object> run=basic(id); setState(id,run,"FAILED",code,Map.of()); }
    private void setState(UUID id,Map<String,Object> run,String next,String type,Map<String,Object> payload) {
        String previous=(String)run.get("status"); String error=next.equals("FAILED")?type:null;
        int changed=jdbc.update("update office.team_runs set status=?,error_code=?,updated_at=? where id=? and status=?",next,error,time(Instant.now()),id,previous);
        if(changed==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Team run state changed; reload and retry");
        transitionEvent(id,(UUID)run.get("project_id"),previous,next,type,payload);
    }
    private void markRemaining(UUID id,int attempt,int failedAt,String status) {
        Instant now=Instant.now();
        Integer last=jdbc.queryForObject("select coalesce(max(step_number),0) from office.team_steps where run_id=? and attempt_number=?",Integer.class,id,attempt);
        for(int step=Math.max(failedAt,last)+1;step<=TeamRole.values().length;step++)
            jdbc.update("insert into office.team_steps(id,run_id,attempt_number,step_number,role,status,error_code,started_at,finished_at) values(?,?,?,?,?,?,?,?,?)",
                    UUID.randomUUID(),id,attempt,step,TeamRole.values()[step-1].name(),status,status.equals("SKIPPED")?"PREVIOUS_STEP_FAILED":"CANCELLED",time(now),time(now));
    }
    private void transitionEvent(UUID id,UUID project,String from,String to,String type,Map<String,Object> payload) {
        try {
            Map<String,Object> body=new LinkedHashMap<>(payload); body.put("fromStatus",from); body.put("toStatus",to);
            UUID eventId=UUID.randomUUID(); Instant occurred=Instant.now(); String json=mapper.writeValueAsString(body);
            jdbc.update("insert into office.project_events(event_id,project_id,execution_id,event_type,payload_json,occurred_at) values(?,?,?,?,?,?)",eventId,project,id,type,json,time(occurred));
            Long cursor=jdbc.queryForObject("select cursor from office.project_events where event_id=?",Long.class,eventId);
            publisher.publishEvent(new ProjectEvent(cursor,eventId,project,id,type,body,occurred));
        } catch(Exception ex) { throw new IllegalStateException("Could not persist team event",ex); }
    }
    private Map<String,Object> basic(UUID id) {
        List<Map<String,Object>> rows=jdbc.query("select * from office.team_runs where id=?",(rs,n)->{
            Map<String,Object> r=new LinkedHashMap<>(); r.put("id",rs.getObject("id",UUID.class)); r.put("project_id",rs.getObject("project_id",UUID.class)); r.put("demand",rs.getString("demand")); r.put("idempotency_key",rs.getString("idempotency_key")); r.put("status",rs.getString("status")); r.put("attempt_count",rs.getInt("attempt_count")); r.put("artifact_json",rs.getString("artifact_json")); r.put("artifact_hash",rs.getString("artifact_hash")); r.put("model",rs.getString("model")); r.put("context_version",rs.getString("context_version")); java.sql.Timestamp expiry=rs.getTimestamp("expires_at"); r.put("expires_at",expiry==null?null:expiry.toInstant()); r.put("error_code",rs.getString("error_code")); r.put("created_at",rs.getTimestamp("created_at").toInstant()); r.put("updated_at",rs.getTimestamp("updated_at").toInstant()); return r;
        },id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Team run not found"); return rows.getFirst();
    }
    private String write(Object value) { try { return mapper.writeValueAsString(value); } catch(Exception ex) { throw new IllegalArgumentException(ex); } }
    private Object parseJson(String value) { try { return mapper.readTree(value); } catch(Exception ex) { throw new IllegalStateException("Stored event payload is invalid",ex); } }
    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception ex) { throw new IllegalStateException(ex); } }
    private java.sql.Timestamp time(Instant instant) { return java.sql.Timestamp.from(instant); }
}
