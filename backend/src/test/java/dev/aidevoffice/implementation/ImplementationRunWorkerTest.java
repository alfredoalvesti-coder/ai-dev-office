package dev.aidevoffice.implementation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImplementationRunWorkerTest {
    @Test
    void retriesInvalidGeneratorOutputOnce() {
        ImplementationRunService runs = mock(ImplementationRunService.class);
        ImplementationGenerator generator = mock(ImplementationGenerator.class);
        UUID id = UUID.randomUUID();
        UUID project = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();

        when(runs.start(id)).thenReturn(true);
        when(runs.context(id)).thenReturn(new ImplementationRunService.GenerationContext(
                project, UUID.randomUUID(), "CRUD de notas", "{\"summary\":\"plano\"}"));
        when(generator.generate(anyString(), anyString())).thenAnswer(invocation -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalArgumentException("OUTPUT_INVALID");
            }
            return new ImplementationProposal("Notas", List.of(
                    new ProposedFile("README.md", "# Notas", "doc")));
        });
        when(generator.model()).thenReturn("qwen2.5:3b");
        doNothing().when(runs).complete(eq(id), any(ImplementationProposal.class), eq("qwen2.5:3b"));

        new ImplementationRunWorker(runs, generator, 5)
                .queued(new ImplementationRunService.ImplementationQueued(id));

        verify(generator, times(2)).generate(anyString(), anyString());
        verify(runs).complete(eq(id), any(ImplementationProposal.class), eq("qwen2.5:3b"));
        verify(runs, never()).fail(eq(id), anyString());
    }
}
