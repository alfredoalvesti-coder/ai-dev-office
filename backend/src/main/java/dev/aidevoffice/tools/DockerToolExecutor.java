package dev.aidevoffice.tools;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@Component
class DockerToolExecutor {
    private static final String CONTAINER_PREFIX = "ai-dev-office-run-";
    private static final Duration DEADLINE = Duration.ofSeconds(10);
    private static final int MAX_OUTPUT = 8192;

    Result execute(ToolRunService.Run run, BooleanSupplier cancelled) {
        long started = System.nanoTime();
        if (cancelled.getAsBoolean()) {
            return stopped("CANCELLED", null, started);
        }
        if (!imageIsUsable(run.image())) {
            return stopped("FAILED", "IMAGE_UNAVAILABLE", started);
        }
        if (cancelled.getAsBoolean()) {
            return stopped("CANCELLED", null, started);
        }
        try (var streams = Executors.newVirtualThreadPerTaskExecutor()) {
            long containerStarted = System.nanoTime();
            Process process = new ProcessBuilder(command(run.id(), run.tool(), run.image()))
                    .redirectErrorStream(true).start();
            Future<Output> output = streams.submit(() -> read(process.getInputStream()));
            Future<?> input = streams.submit(() -> {
                try (var stdin = process.getOutputStream()) {
                    stdin.write(run.source().getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                    // Startup and early exits are reported by the Docker process.
                }
            });
            try {
                while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                    if (cancelled.getAsBoolean()) {
                        return stopped("CANCELLED", null, started);
                    }
                    if (elapsed(containerStarted) >= DEADLINE.toMillis()) {
                        return stopped("TIMED_OUT", "TIME_LIMIT", started);
                    }
                }
                int code = process.exitValue();
                Output captured = output.get(2, TimeUnit.SECONDS);
                if (code == 125 && captured.text().startsWith("docker:")) {
                    return new Result("FAILED", code, "", captured.truncated(), "EXECUTOR_UNAVAILABLE", elapsed(started));
                }
                return new Result(code == 0 ? "SUCCEEDED" : "FAILED", code, captured.text(),
                        captured.truncated(), code == 0 ? null : "NONZERO_EXIT", elapsed(started));
            } finally {
                input.cancel(true);
                output.cancel(true);
                if (process.isAlive()) {
                    process.destroyForcibly();
                    process.getInputStream().close();
                    process.getOutputStream().close();
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return stopped("FAILED", "EXECUTOR_INTERRUPTED", started);
        } catch (Exception exception) {
            return stopped("FAILED", "EXECUTOR_UNAVAILABLE", started);
        }
    }

    static List<String> command(UUID id, ToolRunService.Tool tool, String image) {
        List<String> args = new ArrayList<>(List.of("docker", "run", "--rm", "--interactive", "--pull=never",
                "--name", container(id), "--label", "dev.aidevoffice.tool-run=" + id,
                "--network=none", "--read-only", "--user=1000:1000", "--workdir=/tmp",
                "--env", "HOME=/tmp", "--cap-drop=ALL", "--security-opt=no-new-privileges",
                "--security-opt=seccomp=builtin", "--cpus=0.50", "--memory=128m", "--memory-swap=128m",
                "--pids-limit=32", "--ulimit", "nofile=128:128", "--ulimit", "core=0:0",
                "--shm-size=4m", "--tmpfs", "/tmp:rw,noexec,nosuid,nodev,size=8m,mode=1777",
                "--log-driver=none", "--no-healthcheck"));
        // Docker CLI proxy configuration can otherwise inject credentials even without a network.
        for (String proxy : List.of("HTTP_PROXY", "HTTPS_PROXY", "NO_PROXY", "FTP_PROXY", "ALL_PROXY")) {
            args.add("--env=" + proxy + "=");
            args.add("--env=" + proxy.toLowerCase(Locale.ROOT) + "=");
        }
        args.addAll(List.of("--entrypoint=/usr/bin/timeout", image, "--foreground", "--signal=KILL",
                "15s", "/usr/local/bin/node", "--max-old-space-size=64"));
        args.add(tool == ToolRunService.Tool.NODE_CHECK ? "--check" : "--input-type=commonjs");
        args.add("-");
        return List.copyOf(args);
    }

    boolean cleanup(UUID id) {
        CliResult result = cli("container", "rm", "--force", container(id));
        return result.code() == 0 || result.output().toLowerCase(Locale.ROOT).contains("no such container");
    }

    List<UUID> containers() {
        CliResult result = cli("container", "ls", "--all", "--filter", "label=dev.aidevoffice.tool-run",
                "--format", "{{.Names}}");
        if (result.code() != 0) {
            throw new IllegalStateException("Não foi possível consultar contêineres do executor");
        }
        return result.output().lines()
                .filter(name -> name.matches(CONTAINER_PREFIX
                        + "[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}"))
                .map(name -> UUID.fromString(name.substring(CONTAINER_PREFIX.length())))
                .toList();
    }

    private boolean imageIsUsable(String image) {
        if (image == null) {
            return false;
        }
        CliResult docker = cli("info", "--format",
                "{{.OSType}} {{.CgroupVersion}} {{.MemoryLimit}} {{.SwapLimit}} {{.CPUCfsQuota}} {{.PidsLimit}}"
                        + " {{json .SecurityOptions}}");
        if (docker.code() != 0 || docker.output().lines().noneMatch(line ->
                line.startsWith("linux 2 true true true true [") && line.contains("\"name=seccomp"))) {
            return false;
        }
        CliResult result = cli("image", "inspect", "--format", "{{json .Config.Volumes}}", image);
        String volumes = result.output().strip();
        return result.code() == 0 && (volumes.equals("null") || volumes.equals("{}"));
    }

    private CliResult cli(String... arguments) {
        List<String> command = new ArrayList<>(List.of("docker"));
        command.addAll(List.of(arguments));
        Process process = null;
        try (var streams = Executors.newVirtualThreadPerTaskExecutor()) {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            InputStream stdout = process.getInputStream();
            Future<Output> output = streams.submit(() -> read(stdout));
            try {
                if (process.waitFor(4, TimeUnit.SECONDS)) {
                    return new CliResult(process.exitValue(), output.get(1, TimeUnit.SECONDS).text());
                }
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                stdout.close();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // A daemon outage leaves the execution recoverable by the scheduler.
        }
        return new CliResult(-1, "");
    }

    static Output read(InputStream input) throws IOException {
        ByteArrayOutputStream captured = new ByteArrayOutputStream(MAX_OUTPUT);
        boolean truncated = false;
        byte[] chunk = new byte[1024];
        int count;
        while ((count = input.read(chunk)) != -1) {
            int keep = Math.min(count, MAX_OUTPUT - captured.size());
            captured.write(chunk, 0, keep);
            truncated |= count > keep;
        }
        String text = captured.toString(StandardCharsets.UTF_8).replaceAll("[\\p{Cc}&&[^\\n\\r\\t]]", "");
        return new Output(text, truncated);
    }

    private static String container(UUID id) {
        return CONTAINER_PREFIX + id;
    }

    private static long elapsed(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private static Result stopped(String status, String error, long started) {
        return new Result(status, null, "", false, error, elapsed(started));
    }

    record Result(String status, Integer exitCode, String output, boolean truncated, String errorCode, long elapsedMs) {}
    record Output(String text, boolean truncated) {}
    private record CliResult(int code, String output) {}
}
