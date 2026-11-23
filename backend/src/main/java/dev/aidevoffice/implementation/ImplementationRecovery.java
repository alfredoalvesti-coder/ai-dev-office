package dev.aidevoffice.implementation;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ImplementationRecovery {
    private final ImplementationRunService runs;

    public ImplementationRecovery(ImplementationRunService runs) {
        this.runs = runs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        runs.recoverInterrupted();
    }
}
