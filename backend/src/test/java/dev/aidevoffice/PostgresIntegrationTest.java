package dev.aidevoffice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.hikari.connection-timeout=1000",
        "app.security.username=postgres-integration",
        "app.security.password=postgres-integration-test-only"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.12-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Flyway flyway;

    @Autowired
    MockMvc mvc;

    @Test
    void migrationsAreRepeatableAndReadinessDetectsDatabaseFailure() throws Exception {
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'office'",
                Integer.class)).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        postgres.stop();
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
    }
}
