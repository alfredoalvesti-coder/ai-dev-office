package dev.aidevoffice.tools;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DockerToolExecutorTest {
    @Test
    void passesOnlyFixedArgumentsAndKeepsSourceOffTheCommandLine() {
        var command = DockerToolExecutor.command(UUID.randomUUID(), ToolRunService.Tool.NODE_CHECK, "sha256:test");
        assertThat(command).contains("--network=none", "--read-only", "--pull=never", "--cap-drop=ALL",
                "--security-opt=no-new-privileges", "--pids-limit=32", "--log-driver=none",
                "--env=HTTPS_PROXY=", "--env=https_proxy=");
        assertThat(command).doesNotContain("--volume", "--mount", "--privileged");
        assertThat(command).endsWith("--max-old-space-size=64", "--check", "-");
    }

    @Test
    void capsOutputAndStripsControlCharacters() throws Exception {
        var bytes = ("safe\u0000\u001b\n" + "x".repeat(16_384)).getBytes(StandardCharsets.UTF_8);
        var output = DockerToolExecutor.read(new ByteArrayInputStream(bytes));
        assertThat(output.truncated()).isTrue();
        assertThat(output.text()).startsWith("safe\n").hasSize(8190);
    }
}
