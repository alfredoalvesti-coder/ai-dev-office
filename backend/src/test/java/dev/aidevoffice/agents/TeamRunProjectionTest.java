package dev.aidevoffice.agents;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:team-projection;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TeamRunProjectionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    void projectsCaseSensitiveStepKeysForTheCurrentOfficeView() throws Exception {
        var project = UUID.randomUUID();
        var run = UUID.randomUUID();
        jdbc.update("""
                insert into office.projects (id,name,created_at,updated_at)
                values (?,'Projeto de teste',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, project);
        jdbc.update("""
                insert into office.team_runs
                    (id,project_id,demand,idempotency_key,status,attempt_count,context_version,created_at,updated_at)
                values (?,?,'Demanda de teste','teste-projecao','RUNNING',2,'test',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, run, project);
        jdbc.update("""
                insert into office.team_steps (id,run_id,attempt_number,step_number,role,status,started_at)
                values (?,?,2,1,'PROJECT_MANAGER','RUNNING',CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), run);

        mvc.perform(get("/api/projects/{project}/team-runs", project).with(httpBasic("tester", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attempt_count").value(2))
                .andExpect(jsonPath("$[0].steps[0].attemptNumber").value(2))
                .andExpect(jsonPath("$[0].steps[0].stepNumber").value(1))
                .andExpect(jsonPath("$[0].steps[0].role").value("PROJECT_MANAGER"))
                .andExpect(jsonPath("$[0].steps[0].status").value("RUNNING"))
                .andExpect(jsonPath("$[0].steps[0].startedAt").exists());
    }
}
