package dev.aidevoffice.agents;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectEventsConfigurationTest {
    @Test
    void allowsOnlyTheTwoLocalAngularDevelopmentOrigins() {
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        when(registry.addHandler(any(WebSocketHandler.class), eq("/ws/projects/{projectId}")))
                .thenReturn(registration);
        when(registration.addInterceptors(any())).thenReturn(registration);
        when(registration.setHandshakeHandler(any())).thenReturn(registration);
        when(registration.setAllowedOrigins("http://localhost:4200", "http://127.0.0.1:4200"))
                .thenReturn(registration);

        ProjectEventsConfiguration configuration = new ProjectEventsConfiguration(
                new ProjectEventSocket(new ObjectMapper()), new WebSocketTicketService(),
                new String[] {"http://localhost:4200", "http://127.0.0.1:4200"});

        configuration.registerWebSocketHandlers(registry);

        verify(registration).setAllowedOrigins("http://localhost:4200", "http://127.0.0.1:4200");
    }
}
