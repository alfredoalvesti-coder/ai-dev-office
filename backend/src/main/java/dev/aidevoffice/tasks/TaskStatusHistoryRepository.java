package dev.aidevoffice.tasks;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, UUID> {
    List<TaskStatusHistory> findByTaskIdOrderByChangedAtAscIdAsc(UUID taskId);
}
