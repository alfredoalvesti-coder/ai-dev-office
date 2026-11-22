package dev.aidevoffice.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ProjectEventSocket extends TextWebSocketHandler {
    private final ObjectMapper mapper;
    private final Map<UUID,CopyOnWriteArraySet<WebSocketSession>> sessions=new ConcurrentHashMap<>();
    public ProjectEventSocket(ObjectMapper mapper) { this.mapper=mapper; }

    @Override public void afterConnectionEstablished(WebSocketSession session) throws java.io.IOException {
        UUID project=(UUID)session.getAttributes().get("projectId");
        var listeners=sessions.computeIfAbsent(project,ignored->new CopyOnWriteArraySet<>());
        if(listeners.size()>=10) { session.close(CloseStatus.SERVICE_RESTARTED.withReason("Project event connection limit reached")); return; }
        listeners.add(session);
    }
    @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status) { remove(session); }
    @Override public void handleTransportError(WebSocketSession session,Throwable exception) { remove(session); }
    public void publish(TeamRunService.ProjectEvent event) {
        var listeners=sessions.get(event.projectId()); if(listeners==null) return;
        try {
            String json=mapper.writeValueAsString(Map.of("cursor",event.cursor(),"eventId",event.eventId(),"projectId",event.projectId(),"executionId",event.executionId(),"type",event.type(),"payload",event.payload(),"occurredAt",event.occurredAt()));
            TextMessage message=new TextMessage(json);
            for(WebSocketSession session:listeners) try { if(session.isOpen()) synchronized(session) { session.sendMessage(message); } else remove(session); }
            catch(Exception ex) { remove(session); }
        } catch(Exception ignored) { /* event remains available from persisted cursor history */ }
    }
    private void remove(WebSocketSession session) {
        Object value=session.getAttributes().get("projectId"); if(value instanceof UUID project) {
            var listeners=sessions.get(project); if(listeners!=null) { listeners.remove(session); if(listeners.isEmpty()) sessions.remove(project,listeners); }
        }
    }
}
