package dev.aidevoffice.tasks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @EntityGraph(attributePaths = {"dependencies", "acceptanceCriteria", "project"})
    List<Task> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    @EntityGraph(attributePaths = {"dependencies", "acceptanceCriteria", "project"})
    Optional<Task> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = "dependencies")
    List<Task> findAllByProjectId(UUID projectId);
}
