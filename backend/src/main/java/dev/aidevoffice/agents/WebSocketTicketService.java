package dev.aidevoffice.agents;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebSocketTicketService {
    private final ConcurrentHashMap<String,Ticket> tickets=new ConcurrentHashMap<>();
    public String issue(UUID projectId,String username) {
        tickets.entrySet().removeIf(entry->entry.getValue().expiresAt().isBefore(Instant.now()));
        String value=UUID.randomUUID()+"."+UUID.randomUUID();
        tickets.put(value,new Ticket(projectId,username,Instant.now().plusSeconds(60))); return value;
    }
    public Ticket consume(String value,UUID projectId) {
        Ticket ticket=tickets.remove(value);
        if(ticket==null||ticket.expiresAt().isBefore(Instant.now())||!ticket.projectId().equals(projectId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"WebSocket ticket invalid or expired");
        return ticket;
    }
    public record Ticket(UUID projectId,String username,Instant expiresAt) {}
}
