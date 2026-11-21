package dev.aidevoffice.platform;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("AI Dev Office API")
                .version("0.1.0")
                .description("Fundação da plataforma. APIs de gestão serão adicionadas na Fase 2."));
    }
}
