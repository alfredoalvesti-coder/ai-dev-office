package dev.aidevoffice.agents;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@Configuration
@EnableAsync
public class AgentConfiguration implements AsyncConfigurer {
    @Bean(name = "agentRunExecutor")
    @Override
    public Executor getAsyncExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "agent-runner");
            thread.setDaemon(true);
            return thread;
        });
    }
}
