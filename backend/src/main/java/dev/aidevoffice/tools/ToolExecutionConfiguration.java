package dev.aidevoffice.tools;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
class ToolExecutionConfiguration {
    @Bean(destroyMethod = "shutdownNow")
    ExecutorService toolRunExecutor() {
        return Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("tool-runner").factory());
    }
}
