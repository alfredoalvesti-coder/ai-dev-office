package dev.aidevoffice.implementation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceApplierTest {
    @TempDir Path temp;

    @Test
    void appliesSafeFilesAndRejectsTraversal() throws Exception {
        WorkspaceApplier applier = new WorkspaceApplier(temp.toString());
        UUID project = UUID.randomUUID();
        ImplementationProposal proposal = new ImplementationProposal("CRUD de notas", List.of(
                new ProposedFile("README.md", "# Notas\n", "Visão"),
                new ProposedFile("src/Note.java", "public class Note {}", "Entidade")));

        List<String> written = applier.apply(project, proposal);
        assertThat(written).containsExactlyInAnyOrder("README.md", "src/Note.java");
        assertThat(Files.readString(temp.resolve(project.toString()).resolve("README.md"))).contains("Notas");

        assertThatThrownBy(() -> applier.apply(project, new ImplementationProposal("x",
                List.of(new ProposedFile("../secret.txt", "no", "")))))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> applier.apply(project, new ImplementationProposal("x",
                List.of(new ProposedFile("src/app.exe", "no", "")))))
                .isInstanceOf(ResponseStatusException.class);
    }
}
