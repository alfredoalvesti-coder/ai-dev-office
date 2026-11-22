package dev.aidevoffice.agents;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ProjectEventPublisher {
    private final ProjectEventSocket socket;
    public ProjectEventPublisher(ProjectEventSocket socket) { this.socket=socket; }
    @TransactionalEventListener public void afterCommit(TeamRunService.ProjectEvent event) { socket.publish(event); }
}
