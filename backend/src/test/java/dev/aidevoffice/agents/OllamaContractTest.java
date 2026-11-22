package dev.aidevoffice.agents;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OllamaContractTest {
    private static final String MODEL = "test-model";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicReference<JsonNode> REQUEST = new AtomicReference<>();
    private static final HttpServer OLLAMA = startStub();

    @Autowired AnalysisGenerator analyst;
    @Autowired TeamAnalysisGenerator team;

    @DynamicPropertySource
    static void selectStub(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", () -> "http://127.0.0.1:" + OLLAMA.getAddress().getPort());
        registry.add("spring.ai.ollama.chat.model", () -> MODEL);
    }

    @AfterAll
    static void stopStub() {
        OLLAMA.stop(0);
    }

    @Test
    void sendsConfiguredModelAndParsesStructuredResponsesForBothAdapters() {
        var analysis = analyst.generate("Catalogar livros");
        assertThat(analysis.model()).isEqualTo(MODEL);
        assertThat(analysis.draft().summary()).isEqualTo("Catálogo");
        assertThat(analysis.draft().tasks().getFirst().acceptanceCriteria()).containsExactly("Exibir título");
        assertRequest(1200);

        var result = team.generate(TeamRole.BUSINESS_ANALYST, "Catalogar livros", "[]");
        assertThat(result.summary()).isEqualTo("Catálogo");
        assertThat(result.tasks()).hasSize(1);
        assertThat(result.findings()).isEmpty();
        assertRequest(800);
    }

    private static void assertRequest(int tokenLimit) {
        var request = REQUEST.get();
        assertThat(request.path("model").stringValue()).isEqualTo(MODEL);
        assertThat(request.path("format").stringValue()).isEqualTo("json");
        assertThat(request.path("options").path("num_predict").intValue()).isEqualTo(tokenLimit);
    }

    private static HttpServer startStub() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/chat", exchange -> {
                try (exchange) {
                    var request = JSON.readTree(exchange.getRequestBody());
                    REQUEST.set(request);
                    String artifact = """
                            {"summary":"Catálogo","tasks":[{"title":"Listar livros","description":"Criar listagem",
                            "acceptanceCriteria":["Exibir título"]}]}
                            """;
                    byte[] body = JSON.writeValueAsBytes(Map.of(
                            "model", MODEL, "created_at", "2026-01-01T00:00:00Z",
                            "message", Map.of("role", "assistant", "content", artifact),
                            "done", true, "done_reason", "stop", "prompt_eval_count", 7, "eval_count", 5));
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao iniciar o Ollama de teste", exception);
        }
    }
}
