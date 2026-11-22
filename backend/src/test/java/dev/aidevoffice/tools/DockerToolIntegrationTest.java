package dev.aidevoffice.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@EnabledIfEnvironmentVariable(named = "APP_TOOLS_TEST_IMAGE", matches = "sha256:[a-f0-9]{64}")
class DockerToolIntegrationTest {
    private final DockerToolExecutor executor = new DockerToolExecutor();

    @Test
    void runsWithoutHostFilesCredentialsOrExternalNetwork() {
        var result = execute("""
                const assert = require('node:assert/strict');
                const fs = require('node:fs');
                assert.equal(process.getuid(), 1000);
                const seccomp = fs.readFileSync('/proc/self/status', 'utf8').split(String.fromCharCode(10))
                  .find(line => line.startsWith('Seccomp:'));
                assert.equal(seccomp.split(':')[1].trim(), '2');
                assert.equal(process.env.APP_PASSWORD, undefined);
                assert.equal(process.env.POSTGRES_PASSWORD, undefined);
                for (const key of ['HTTP_PROXY', 'HTTPS_PROXY', 'NO_PROXY', 'FTP_PROXY', 'ALL_PROXY']) {
                  assert.equal(process.env[key], '');
                  assert.equal(process.env[key.toLowerCase()], '');
                }
                assert.throws(() => fs.readFileSync('/var/run/docker.sock'), { code: 'ENOENT' });
                assert.throws(() => fs.writeFileSync('/tmp/../../forbidden', 'x'),
                  error => ['EROFS', 'EACCES'].includes(error.code));
                assert.throws(() => fs.writeFileSync('/tmp/large', Buffer.alloc(9 * 1024 * 1024)), { code: 'ENOSPC' });
                assert.deepEqual(Object.keys(require('node:os').networkInterfaces()), ['lo']);
                console.log('isolamento confirmado');
                """, () -> false);
        assertThat(result.status()).as(result.output()).isEqualTo("SUCCEEDED");
        assertThat(result.output()).contains("isolamento confirmado");
    }

    @Test
    void enforcesMemoryAndTimeLimits() {
        var memory = execute("Buffer.alloc(256 * 1024 * 1024).fill(42);", () -> false);
        assertThat(memory.status()).isEqualTo("FAILED");
        assertThat(memory.exitCode()).isEqualTo(137);
        var timedOut = execute("while (true) {}", () -> false);
        assertThat(timedOut.status()).isEqualTo("TIMED_OUT");
    }

    @Test
    void cancelsAndRemovesContainer() {
        var checks = new AtomicInteger();
        var result = execute("setInterval(() => console.log('active'), 10);", () -> checks.incrementAndGet() > 12);
        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    private DockerToolExecutor.Result execute(String source, BooleanSupplier cancelled) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        var run = new ToolRunService.Run(id, UUID.randomUUID(), ToolRunService.Tool.NODE_RUN, source,
                "test", System.getenv("APP_TOOLS_TEST_IMAGE"), "RUNNING", "tester", "tester",
                now, now, now, null, null, false, null, null);
        try {
            return executor.execute(run, cancelled);
        } finally {
            assertThat(executor.cleanup(id)).isTrue();
        }
    }
}
