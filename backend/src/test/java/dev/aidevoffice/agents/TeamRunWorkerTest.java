package dev.aidevoffice.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class TeamRunWorkerTest {
    @Test
    void retriesWhenGeneratedArtifactFailsValidation() throws Exception {
        TeamRunService runs = mock(TeamRunService.class);
        TeamAnalysisGenerator generator = mock(TeamAnalysisGenerator.class);
        UUID runId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        AtomicInteger analystCalls = new AtomicInteger();

        when(runs.start(runId)).thenReturn(true);
        when(runs.context(runId)).thenReturn(new TeamRunService.TeamRunContext(
                projectId, "Uma demanda simples", 1, null, List.of()));
        when(runs.beginStep(eq(runId), eq(1), anyInt(), any(TeamRole.class))).thenReturn(true);
        when(generator.generate(any(TeamRole.class), anyString(), anyString())).thenAnswer(invocation -> {
            TeamRole role = invocation.getArgument(0);
            if (role == TeamRole.BUSINESS_ANALYST) analystCalls.incrementAndGet();
            return new TeamStepArtifact("Resumo de " + role, List.of(), List.of(), "Revisar com a pessoa usuária.");
        });
        doThrow(new IllegalArgumentException("OUTPUT_INVALID"))
                .doNothing()
                .when(runs).stepSucceeded(eq(runId), eq(1), eq(2), eq(TeamRole.BUSINESS_ANALYST), any(TeamStepArtifact.class));
        when(generator.model()).thenReturn("qwen2.5:3b");

        new TeamRunWorker(runs, generator, new ObjectMapper(), 5)
                .queued(new TeamRunService.TeamQueued(runId));

        verify(generator, times(2)).generate(eq(TeamRole.BUSINESS_ANALYST), eq("Uma demanda simples"), anyString());
        verify(runs, times(7)).stepSucceeded(eq(runId), eq(1), anyInt(), any(TeamRole.class), any(TeamStepArtifact.class));
        verify(runs).complete(eq(runId), eq(1), anyList(), eq("qwen2.5:3b"));
        verify(runs, never()).fail(eq(runId), eq(1), anyInt(), anyString());
    }

    @Test
    void retriesOneInvalidRoleOutputBeforeFailingTheTeamRun() throws Exception {
        TeamRunService runs = mock(TeamRunService.class);
        TeamAnalysisGenerator generator = mock(TeamAnalysisGenerator.class);
        UUID runId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        AtomicInteger analystCalls = new AtomicInteger();

        when(runs.start(runId)).thenReturn(true);
        when(runs.context(runId)).thenReturn(new TeamRunService.TeamRunContext(
                projectId, "Uma demanda simples", 1, null, List.of()));
        when(runs.beginStep(eq(runId), eq(1), anyInt(), any(TeamRole.class))).thenReturn(true);
        when(generator.generate(any(TeamRole.class), anyString(), anyString())).thenAnswer(invocation -> {
            TeamRole role = invocation.getArgument(0);
            if (role == TeamRole.BUSINESS_ANALYST && analystCalls.getAndIncrement() == 0) {
                throw new IllegalArgumentException("OUTPUT_INVALID");
            }
            return new TeamStepArtifact("Resumo de " + role, List.of(), List.of(), "Revisar com a pessoa usuária.");
        });
        when(generator.model()).thenReturn("qwen2.5:3b");

        new TeamRunWorker(runs, generator, new ObjectMapper(), 5)
                .queued(new TeamRunService.TeamQueued(runId));

        verify(generator, times(2)).generate(eq(TeamRole.BUSINESS_ANALYST), eq("Uma demanda simples"), anyString());
        verify(runs, times(6)).stepSucceeded(eq(runId), eq(1), anyInt(), any(TeamRole.class), any(TeamStepArtifact.class));
        verify(runs).complete(eq(runId), eq(1), anyList(), eq("qwen2.5:3b"));
        verify(runs, never()).fail(eq(runId), eq(1), anyInt(), anyString());
    }
}
