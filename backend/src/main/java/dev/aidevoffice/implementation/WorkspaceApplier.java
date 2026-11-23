package dev.aidevoffice.implementation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WorkspaceApplier {
    private static final Set<String> EXTENSIONS = Set.of(
            "md", "txt", "java", "ts", "html", "css", "json", "yml", "yaml", "sql");
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern HIDDEN = Pattern.compile(
            "[\\p{Cc}&&[^\\t\\r\\n]]|[\\u061c\\u200e\\u200f\\u202a-\\u202e\\u2066-\\u2069]");

    private final Path root;

    public WorkspaceApplier(@Value("${app.implementation.workspace-root:./workspaces}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public String relativeWorkspace(UUID projectId) {
        return projectId.toString();
    }

    public List<String> apply(UUID projectId, ImplementationProposal proposal) {
        Path projectRoot = projectRoot(projectId);
        try {
            Files.createDirectories(projectRoot);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create workspace");
        }
        List<String> written = new ArrayList<>();
        for (ProposedFile file : proposal.files()) {
            Path target = resolve(projectRoot, file.path());
            validateContent(file.content());
            try {
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.content(), StandardCharsets.UTF_8);
                written.add(projectRoot.relativize(target).toString().replace('\\', '/'));
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not write " + file.path());
            }
        }
        return written;
    }

    public List<String> list(UUID projectId) {
        Path projectRoot = projectRoot(projectId);
        if (!Files.isDirectory(projectRoot)) {
            return List.of();
        }
        try (var stream = Files.walk(projectRoot)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> projectRoot.relativize(path).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not list workspace");
        }
    }

    public String read(UUID projectId, String relativePath) {
        Path target = resolve(projectRoot(projectId), relativePath);
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
        }
    }

    void validateProposal(ImplementationProposal proposal) {
        if (proposal.files() == null || proposal.files().isEmpty() || proposal.files().size() > 8) {
            throw new IllegalArgumentException("OUTPUT_INVALID");
        }
        try {
            Path probe = projectRoot(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            for (ProposedFile file : proposal.files()) {
                resolve(probe, file.path());
                validateContent(file.content());
            }
        } catch (ResponseStatusException ex) {
            throw new IllegalArgumentException("OUTPUT_INVALID");
        }
    }

    private Path projectRoot(UUID projectId) {
        return root.resolve(projectId.toString()).normalize();
    }

    private Path resolve(Path projectRoot, String relativePath) {
        if (relativePath == null || relativePath.isBlank() || HIDDEN.matcher(relativePath).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":") || normalized.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path traversal rejected");
        }
        String[] parts = normalized.split("/");
        if (parts.length == 0 || parts.length > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        Path current = projectRoot;
        for (String part : parts) {
            if (part.isBlank() || part.equals(".") || part.equals("..") || !SAFE_SEGMENT.matcher(part).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path segment");
            }
            current = current.resolve(part);
        }
        Path normalizedTarget = current.normalize();
        if (!normalizedTarget.startsWith(projectRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path escapes workspace");
        }
        String fileName = normalizedTarget.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File extension required");
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extension not allowed");
        }
        return normalizedTarget;
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || HIDDEN.matcher(content).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file content");
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > 16384) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds 16 KiB");
        }
    }
}
