package dev.aidevoffice.agents;

import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocket
public class ProjectEventsConfiguration implements WebSocketConfigurer {
    private final ProjectEventSocket socket;
    private final WebSocketTicketService tickets;
    private final String[] allowedOrigins;
    public ProjectEventsConfiguration(ProjectEventSocket socket, WebSocketTicketService tickets,
            @Value("${app.websocket.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") String[] allowedOrigins) {
        this.socket = socket;
        this.tickets = tickets;
        this.allowedOrigins = allowedOrigins;
    }

    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(socket,"/ws/projects/{projectId}")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(new TicketInterceptor(tickets))
                .setHandshakeHandler(new DefaultHandshakeHandler() { { setSupportedProtocols("ai-office-ticket"); } });
    }

    private static class TicketInterceptor implements HandshakeInterceptor {
        private final WebSocketTicketService tickets;
        TicketInterceptor(WebSocketTicketService tickets) { this.tickets=tickets; }
        @Override public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,java.util.Map<String,Object> attributes) {
            try {
                String path=request.getURI().getPath(); UUID project=UUID.fromString(path.substring(path.lastIndexOf('/')+1));
                String offered=request.getHeaders().getFirst("Sec-WebSocket-Protocol");
                if(offered==null) return false;
                String ticket=java.util.Arrays.stream(offered.split(",")).map(String::trim).filter(item->item.startsWith("ticket.")).map(item->item.substring(7)).findFirst().orElse(null);
                if(ticket==null) return false;
                WebSocketTicketService.Ticket validated=tickets.consume(ticket,project);
                attributes.put("projectId",validated.projectId()); attributes.put("username",validated.username()); return true;
            } catch(Exception ex) { return false; }
        }
        @Override public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception exception) {}
    }
}
