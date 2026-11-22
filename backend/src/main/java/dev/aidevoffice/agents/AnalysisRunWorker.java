package dev.aidevoffice.agents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class AnalysisRunWorker {
    private final AnalysisRunService runs;
    private final AnalysisGenerator generator;
    private final String model;
    private final int timeoutSeconds;
    private final ExecutorService modelCalls=Executors.newSingleThreadExecutor(r -> { Thread thread=new Thread(r,"ollama-call"); thread.setDaemon(true); return thread; });
    public AnalysisRunWorker(AnalysisRunService runs, AnalysisGenerator generator,
            @Value("${spring.ai.ollama.chat.model}") String model,
            @Value("${app.agents.timeout-seconds:180}") int timeoutSeconds) {
        this.runs=runs; this.generator=generator; this.model=model; this.timeoutSeconds=timeoutSeconds;
    }

    @Async("agentRunExecutor")
    @TransactionalEventListener
    public void queued(AnalysisRunService.QueuedAnalysis event) {
        Future<AnalysisGenerator.GeneratedAnalysis> call=null;
        try {
            if(!runs.start(event.id(),model)) return;
            call=modelCalls.submit(() -> generator.generate(runs.demand(event.id())));
            AnalysisGenerator.GeneratedAnalysis result=call.get(timeoutSeconds, TimeUnit.SECONDS);
            runs.complete(event.id(),result);
        } catch(TimeoutException ex) { if(call!=null) call.cancel(true); runs.fail(event.id(),"TIMEOUT"); }
        catch(InterruptedException ex) { Thread.currentThread().interrupt(); runs.fail(event.id(),"CANCELLED"); }
        catch(ExecutionException ex) {
            if(ex.getCause() instanceof IllegalArgumentException) runs.fail(event.id(),"OUTPUT_INVALID");
            else runs.fail(event.id(),"OLLAMA_UNAVAILABLE");
        } catch(IllegalArgumentException ex) { runs.fail(event.id(),"OUTPUT_INVALID"); }
        catch(Exception ex) { runs.fail(event.id(),"OLLAMA_UNAVAILABLE"); }
    }
}
