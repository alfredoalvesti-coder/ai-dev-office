package dev.aidevoffice.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
class ToolRunService {
    private static final Logger LOG = LoggerFactory.getLogger(ToolRunService.class);
    static final String POLICY = "node-isolation-v1";
    static final int INPUT_BYTES = 8192;
    private static final int APPROVAL_SECONDS = 24 * 60 * 60;
    private static final int LEASE_SECONDS = 90;
    private static final Pattern HIDDEN_CONTROLS = Pattern.compile(
            "[\\p{Cc}&&[^\\t\\r\\n]]|[\\u061c\\u200e\\u200f\\u202a-\\u202e\\u2066-\\u2069]");
    private static final String SELECT = "SELECT * FROM office.tool_runs ";
    private static final RowMapper<Run> MAPPER = (row, index) -> new Run(
            row.getObject("id", UUID.class), row.getObject("project_id", UUID.class),
            Tool.valueOf(row.getString("tool")), row.getString("source"), row.getString("review_hash"),
            row.getString("image"), row.getString("status"), row.getString("requested_by"),
            row.getString("approved_by"), timestamp(row.getObject("approved_at", OffsetDateTime.class)),
            timestamp(row.getObject("created_at", OffsetDateTime.class)),
            timestamp(row.getObject("updated_at", OffsetDateTime.class)),
            row.getObject("exit_code", Integer.class), row.getString("output"),
            row.getBoolean("output_truncated"), row.getString("error_code"),
            row.getObject("elapsed_ms", Long.class));

    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final String image;

    ToolRunService(JdbcTemplate jdbc, @Value("${app.tools.enabled}") boolean enabled,
                   @Value("${app.tools.image}") String image) {
        if (enabled && !image.matches("(?:[a-z0-9][a-z0-9./:_-]{0,174}@)?sha256:[a-f0-9]{64}")) {
            throw new IllegalArgumentException("APP_TOOLS_IMAGE precisa identificar uma imagem local por SHA-256");
        }
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.image = enabled ? image : null;
    }

    boolean enabled() {
        return enabled;
    }

    boolean matchesPolicy(Run run) {
        return enabled && image.equals(run.image())
                && run.hash().equals(hash(run.projectId(), run.id(), run.tool(), image, run.source()));
    }

    List<Run> list(UUID project) {
        requireProject(project);
        return jdbc.query(SELECT + "WHERE project_id=? ORDER BY created_at DESC,id DESC LIMIT 20", MAPPER, project);
    }

    Run get(UUID project, UUID id) {
        return jdbc.query(SELECT + "WHERE project_id=? AND id=?", MAPPER, project, id).stream()
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execução inexistente"));
    }

    Run create(UUID project, Tool tool, String source, String actor) {
        requireProject(project);
        if (HIDDEN_CONTROLS.matcher(source).find() || !StandardCharsets.UTF_8.newEncoder().canEncode(source)
                || source.getBytes(StandardCharsets.UTF_8).length > INPUT_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use UTF-8 válido, até 8192 bytes e sem controles invisíveis");
        }
        UUID id = UUID.randomUUID();
        String hash = hash(project, id, tool, image, source);
        OffsetDateTime now = now();
        jdbc.update("""
                INSERT INTO office.tool_runs
                (id,project_id,tool,source,review_hash,image,status,requested_by,created_at,updated_at)
                VALUES (?,?,?,?,?,?,'AWAITING_APPROVAL',?,?,?)
                """, id, project, tool.name(), source, hash, image, actor, now, now);
        LOG.atInfo().addKeyValue("execution.id", id).addKeyValue("project.id", project)
                .addKeyValue("tool", tool).log("Proposta de ferramenta registrada");
        return get(project, id);
    }

    Run approve(UUID project, UUID id, String hash, String actor) {
        Run run = get(project, id);
        if (!run.hash().equals(hash)) {
            throw conflict("Conteúdo diferente da versão revisada");
        }
        if (run.approvedAt() != null) {
            return run;
        }
        OffsetDateTime now = now();
        if (!run.createdAt().isAfter(now.minusSeconds(APPROVAL_SECONDS).toInstant())) {
            jdbc.update("""
                    UPDATE office.tool_runs SET status='EXPIRED',updated_at=?
                    WHERE id=? AND status='AWAITING_APPROVAL'
                    """, now, id);
            throw conflict("Prazo de aprovação expirado");
        }
        if (!matchesPolicy(run)) {
            throw conflict("Executor indisponível para esta proposta");
        }
        int updated = jdbc.update("""
                UPDATE office.tool_runs SET status='QUEUED',approved_by=?,approved_at=?,updated_at=?
                WHERE id=? AND project_id=? AND review_hash=? AND status='AWAITING_APPROVAL'
                """, actor, now, now, id, project, hash);
        if (updated == 1) {
            LOG.atInfo().addKeyValue("execution.id", id).addKeyValue("project.id", project)
                    .log("Execução de ferramenta aprovada");
        }
        Run current = get(project, id);
        if (current.approvedAt() == null) {
            throw conflict("Execução já decidida");
        }
        return current;
    }

    Run cancel(UUID project, UUID id) {
        get(project, id);
        jdbc.update("""
                UPDATE office.tool_runs SET status=CASE WHEN status='RUNNING'
                    THEN 'CANCEL_REQUESTED' ELSE 'CANCELLED' END,updated_at=?
                WHERE id=? AND project_id=? AND status IN ('AWAITING_APPROVAL','QUEUED','RUNNING')
                """, now(), id, project);
        return get(project, id);
    }

    void expireProposals() {
        OffsetDateTime now = now();
        jdbc.update("""
                UPDATE office.tool_runs SET status='EXPIRED',updated_at=?
                WHERE status='AWAITING_APPROVAL' AND created_at<=?
                """, now, now.minusSeconds(APPROVAL_SECONDS));
    }

    List<Run> abandoned() {
        return jdbc.query(SELECT + """
                WHERE status IN ('RUNNING','CANCEL_REQUESTED') AND lease_until<=?
                ORDER BY lease_until LIMIT 10
                """, MAPPER, now());
    }

    boolean isFinished(UUID id) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM office.tool_runs
                WHERE id=? AND status IN ('CANCELLED','SUCCEEDED','FAILED','TIMED_OUT','EXPIRED')
                """, Integer.class, id) == 1;
    }

    boolean claim(Run run) {
        OffsetDateTime now = now();
        return jdbc.update("""
                UPDATE office.tool_runs SET status='RUNNING',updated_at=?,lease_until=?
                WHERE id=? AND status='QUEUED' AND approved_at IS NOT NULL
                """, now, now.plusSeconds(LEASE_SECONDS), run.id()) == 1;
    }

    List<Run> queued() {
        return jdbc.query(SELECT + "WHERE status='QUEUED' ORDER BY approved_at,id LIMIT 10", MAPPER);
    }

    boolean cancelled(Run run) {
        return !"RUNNING".equals(get(run.projectId(), run.id()).status());
    }

    void requireCleanup(Run run) {
        OffsetDateTime now = now();
        jdbc.update("""
                UPDATE office.tool_runs SET updated_at=?,lease_until=?
                WHERE id=? AND status IN ('RUNNING','CANCEL_REQUESTED')
                """, now, now, run.id());
    }

    String finish(Run run, DockerToolExecutor.Result result) {
        int updated = jdbc.update("""
                UPDATE office.tool_runs SET status=CASE WHEN status='CANCEL_REQUESTED' THEN 'CANCELLED'
                    ELSE ? END,updated_at=?,lease_until=NULL,exit_code=?,output=?,output_truncated=?,error_code=?,elapsed_ms=?
                WHERE id=? AND status IN ('RUNNING','CANCEL_REQUESTED')
                """, result.status(), now(), result.exitCode(), result.output(), result.truncated(),
                result.errorCode(), result.elapsedMs(), run.id());
        return updated == 1 ? get(run.projectId(), run.id()).status() : null;
    }

    private void requireProject(UUID project) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM office.projects WHERE id=?", Integer.class, project) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto inexistente");
        }
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static Instant timestamp(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private static String hash(UUID project, UUID id, Tool tool, String image, String source) {
        String payload = String.join("\n", POLICY, project.toString(), id.toString(), tool.name(),
                image == null ? "" : image, source);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    enum Tool { NODE_CHECK, NODE_RUN }

    record Run(UUID id, UUID projectId, Tool tool, String source, String hash, String image, String status,
               String requestedBy, String approvedBy, Instant approvedAt, Instant createdAt, Instant updatedAt,
               Integer exitCode, String output, boolean outputTruncated, String errorCode, Long elapsedMs) {}
}
