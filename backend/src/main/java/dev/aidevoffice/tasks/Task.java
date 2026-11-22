package dev.aidevoffice.tasks;

import dev.aidevoffice.projects.Project;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tasks", schema = "office")
public class Task {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(nullable = false, length = 4000)
    private String description;
    @ElementCollection
    @CollectionTable(name = "task_acceptance_criteria", schema = "office", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "criterion", nullable = false, length = 500)
    @OrderColumn(name = "position")
    private List<String> acceptanceCriteria = new ArrayList<>();
    @ManyToMany
    @JoinTable(name = "task_dependencies", schema = "office",
            joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "dependency_id"))
    private Set<Task> dependencies = new LinkedHashSet<>();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    protected Task() {}

    Task(Project project, String title, String description, List<String> criteria, Set<Task> dependencies) {
        this.id = UUID.randomUUID();
        this.project = project;
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.acceptanceCriteria = criteria == null ? new ArrayList<>() : criteria.stream().map(String::trim).toList();
        this.dependencies = new LinkedHashSet<>(dependencies);
        this.status = TaskStatus.TODO;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    void updateStatus(TaskStatus next) {
        this.status = next;
        this.updatedAt = Instant.now();
    }

    void updateDependencies(Set<Task> dependencies) {
        this.dependencies = new LinkedHashSet<>(dependencies);
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getAcceptanceCriteria() { return List.copyOf(acceptanceCriteria); }
    public Set<Task> getDependencies() { return Set.copyOf(dependencies); }
    public TaskStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version == null ? 0 : version; }
}
