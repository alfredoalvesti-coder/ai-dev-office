package dev.aidevoffice.implementation;

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
public class ImplementationRunWorker {
    private final ImplementationRunService runs;
    private final ImplementationGenerator generator;
    private final int timeoutSeconds;
    private final ExecutorService modelCalls = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "implementation-model-call");
        thread.setDaemon(true);
        return thread;
    });

    public ImplementationRunWorker(ImplementationRunService runs, ImplementationGenerator generator,
            @Value("${app.agents.timeout-seconds:180}") int timeoutSeconds) {
        this.runs = runs;
        this.generator = generator;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Async("agentRunExecutor")
    @TransactionalEventListener
    public void queued(ImplementationRunService.ImplementationQueued event) {
        try {
            if (!runs.start(event.id())) {
                return;
            }
            ImplementationRunService.GenerationContext context = runs.context(event.id());
            ImplementationProposal proposal = generate(context.demand(), context.planJson());
            runs.complete(event.id(), proposal, generator.model());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            runs.fail(event.id(), "CANCELLED");
        } catch (IllegalArgumentException invalid) {
            runs.fail(event.id(), "OUTPUT_INVALID");
        } catch (Exception failed) {
            runs.fail(event.id(), "OLLAMA_UNAVAILABLE");
        }
    }

    private ImplementationProposal generate(String demand, String plan)
            throws InterruptedException, TimeoutException {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                Future<ImplementationProposal> future = modelCalls.submit(() -> generator.generate(demand, plan));
                try {
                    return future.get(timeoutSeconds, TimeUnit.SECONDS);
                } catch (TimeoutException timeout) {
                    future.cancel(true);
                    throw timeout;
                } catch (ExecutionException failed) {
                    if (failed.getCause() instanceof IllegalArgumentException) {
                        throw new IllegalArgumentException("OUTPUT_INVALID");
                    }
                    throw new IllegalStateException("OLLAMA_UNAVAILABLE");
                }
            } catch (IllegalArgumentException invalid) {
                if (attempt > 0) {
                    throw invalid;
                }
            }
        }
        throw new IllegalArgumentException("OUTPUT_INVALID");
    }
}
