package dev.aidevoffice.agents;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TeamRunRecovery {
    private final TeamRunService runs;
    public TeamRunRecovery(TeamRunService runs) { this.runs=runs; }
    @EventListener(ApplicationReadyEvent.class) public void recover() { runs.recoverInterrupted(); }
}
