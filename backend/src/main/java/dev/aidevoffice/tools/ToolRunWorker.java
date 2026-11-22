package dev.aidevoffice.tools;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
class ToolRunWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ToolRunWorker.class);
    private final ToolRunService service;
    private final DockerToolExecutor executor;
    private final Executor workers;
    private final MeterRegistry meters;
    private final ObservationRegistry observations;
    private final AtomicBoolean busy = new AtomicBoolean();
    private Instant nextReconcile = Instant.EPOCH;

    ToolRunWorker(ToolRunService service, DockerToolExecutor executor,
                  @Qualifier("toolRunExecutor") Executor workers, MeterRegistry meters,
                  ObservationRegistry observations) {
        this.service = service;
        this.executor = executor;
        this.workers = workers;
        this.meters = meters;
        this.observations = observations;
    }

    @Scheduled(fixedDelay = 2000)
    void dispatch() {
        if (busy.compareAndSet(false, true)) {
            try {
                workers.execute(() -> {
                    try {
                        dispatchOne();
                    } catch (RuntimeException exception) {
                        LOG.error("Falha na fila de ferramentas", exception);
                        meters.counter("office.tools.dispatch.failures").increment();
                    } finally {
                        busy.set(false);
                    }
                });
            } catch (RuntimeException exception) {
                busy.set(false);
                throw exception;
            }
        }
    }

    private void dispatchOne() {
        service.expireProposals();
        boolean cleanupPending = false;
        for (var run : service.abandoned()) {
            if (executor.cleanup(run.id())) {
                finish(run, new DockerToolExecutor.Result("FAILED", null, "", false, "INTERRUPTED", 0));
            } else {
                cleanupPending = true;
                meters.counter("office.tools.cleanup.failures").increment();
            }
        }
        if (cleanupPending || !service.enabled()) {
            return;
        }
        reconcileFinishedContainers();
        service.queued().stream().filter(service::claim).findFirst().ifPresent(run ->
                Observation.createNotStarted("office.tool.execution", observations)
                        .lowCardinalityKeyValue("tool", run.tool().name())
                        .highCardinalityKeyValue("execution.id", run.id().toString())
                        .observe(() -> execute(run)));
    }

    private void reconcileFinishedContainers() {
        Instant now = Instant.now();
        if (now.isBefore(nextReconcile)) {
            return;
        }
        nextReconcile = now.plusSeconds(30);
        for (var id : executor.containers()) {
            if (service.isFinished(id) && !executor.cleanup(id)) {
                LOG.atWarn().addKeyValue("execution.id", id).log("Contêiner finalizado com limpeza pendente");
                meters.counter("office.tools.cleanup.failures").increment();
            }
        }
    }

    private void execute(ToolRunService.Run run) {
        try (var correlation = MDC.putCloseable("execution.id", run.id().toString())) {
            LOG.info("Executando ferramenta aprovada {}", run.tool());
            var result = service.matchesPolicy(run)
                    ? executor.execute(run, () -> service.cancelled(run))
                    : new DockerToolExecutor.Result("FAILED", null, "", false, "POLICY_CHANGED", 0);
            if ("IMAGE_UNAVAILABLE".equals(result.errorCode()) || "POLICY_CHANGED".equals(result.errorCode())
                    || executor.cleanup(run.id())) {
                finish(run, result);
            } else {
                service.requireCleanup(run);
                LOG.warn("Limpeza pendente para execução {}", run.id());
                meters.counter("office.tools.cleanup.failures").increment();
            }
        }
    }

    private void finish(ToolRunService.Run run, DockerToolExecutor.Result result) {
        String status = service.finish(run, result);
        if (status == null) {
            return;
        }
        meters.counter("office.tools.runs", "tool", run.tool().name(), "status", status).increment();
        meters.timer("office.tools.duration", "tool", run.tool().name()).record(Duration.ofMillis(result.elapsedMs()));
        LOG.atInfo().addKeyValue("execution.id", run.id()).addKeyValue("project.id", run.projectId())
                .addKeyValue("status", status).addKeyValue("error.code", result.errorCode())
                .addKeyValue("duration.ms", result.elapsedMs()).log("Execução de ferramenta concluída");
    }
}
