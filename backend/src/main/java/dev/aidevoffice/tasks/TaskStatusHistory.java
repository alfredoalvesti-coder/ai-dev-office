package dev.aidevoffice.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_status_history", schema = "office")
public class TaskStatusHistory {
    @Id
    private UUID id;
    @Column(name = "task_id", nullable = false)
    private UUID taskId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private TaskStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private TaskStatus toStatus;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected TaskStatusHistory() {}

    TaskStatusHistory(UUID taskId, TaskStatus fromStatus, TaskStatus toStatus) {
        this.id = UUID.randomUUID();
        this.taskId = taskId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public TaskStatus getFromStatus() { return fromStatus; }
    public TaskStatus getToStatus() { return toStatus; }
    public Instant getChangedAt() { return changedAt; }
}
