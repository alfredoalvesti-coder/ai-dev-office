package dev.aidevoffice.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TeamRunWorker {
    private final TeamRunService runs;
    private final TeamAnalysisGenerator generator;
    private final ObjectMapper mapper;
    private final int timeoutSeconds;
    private final ExecutorService modelCalls=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"team-model-call");t.setDaemon(true);return t;});

    public TeamRunWorker(TeamRunService runs,TeamAnalysisGenerator generator,ObjectMapper mapper,
            @Value("${app.agents.timeout-seconds:180}") int timeoutSeconds) {
        this.runs=runs;this.generator=generator;this.mapper=mapper;this.timeoutSeconds=timeoutSeconds;
    }

    @Async("agentRunExecutor")
    @TransactionalEventListener
    public void queued(TeamRunService.TeamQueued event) {
        int attempt=0,activeStep=0;
        try {
            if(!runs.start(event.id())) return;
            TeamRunService.TeamRunContext context=runs.context(event.id()); attempt=context.attempt();
            List<TeamStepArtifact> artifacts=new ArrayList<>();
            for(int index=0;index<TeamRole.values().length;index++) {
                activeStep=index+1; TeamRole role=TeamRole.values()[index];
                if(!runs.beginStep(event.id(),attempt,activeStep,role)) { runs.fail(event.id(),attempt,0,"CANCELLED"); return; }
                String prior=mapper.writeValueAsString(artifacts);
                TeamStepArtifact artifact = generateAndPersistStep(event.id(), attempt, activeStep,
                        role, context.demand(), prior);
                artifacts.add(artifact);
            }
            runs.complete(event.id(),attempt,artifacts,generator.model());
        } catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); if(attempt>0) runs.fail(event.id(),attempt,activeStep,"CANCELLED"); }
        catch(IllegalArgumentException invalid) { if(attempt>0) runs.fail(event.id(),attempt,activeStep,"OUTPUT_INVALID"); }
        catch(Exception failed) { if(attempt>0) runs.fail(event.id(),attempt,activeStep,"OLLAMA_UNAVAILABLE"); }
    }

    private TeamStepArtifact generateAndPersistStep(java.util.UUID runId, int attempt, int step,
            TeamRole role, String demand, String prior)
            throws InterruptedException, TimeoutException {
        for (int outputAttempt = 0; outputAttempt < 2; outputAttempt++) {
            try {
                TeamStepArtifact artifact = generateStep(role, demand, prior);
                runs.stepSucceeded(runId, attempt, step, role, artifact);
                return artifact;
            } catch (IllegalArgumentException invalid) {
                if (outputAttempt > 0) throw invalid;
            }
        }
        throw new IllegalArgumentException("OUTPUT_INVALID");
    }

    private TeamStepArtifact generateStep(TeamRole role, String demand, String prior)
            throws InterruptedException, TimeoutException {
        Future<TeamStepArtifact> future = modelCalls.submit(() -> generator.generate(role, demand, prior));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            future.cancel(true);
            throw timeout;
        } catch (ExecutionException failed) {
            if (failed.getCause() instanceof IllegalArgumentException) throw new IllegalArgumentException("OUTPUT_INVALID");
            throw new IllegalStateException("OLLAMA_UNAVAILABLE");
        }
    }
}
