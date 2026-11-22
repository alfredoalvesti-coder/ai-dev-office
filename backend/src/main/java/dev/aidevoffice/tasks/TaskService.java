package dev.aidevoffice.tasks;

import dev.aidevoffice.projects.Project;
import dev.aidevoffice.projects.ProjectRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TaskService {
    private final TaskRepository tasks;
    private final TaskStatusHistoryRepository history;
    private final ProjectRepository projects;

    public TaskService(TaskRepository tasks, TaskStatusHistoryRepository history, ProjectRepository projects) {
        this.tasks = tasks;
        this.history = history;
        this.projects = projects;
    }

    public TaskResponse create(UUID projectId, TaskRequest request) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        Set<Task> dependencies = loadDependencies(projectId, request.dependencyIds());
        Task task = tasks.saveAndFlush(new Task(project, request.title(), request.description(),
                request.acceptanceCriteria(), dependencies));
        history.save(new TaskStatusHistory(task.getId(), null, TaskStatus.TODO));
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID projectId) {
        if (!projects.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return tasks.findByProjectIdOrderByCreatedAtAsc(projectId).stream().map(TaskResponse::from).toList();
    }

    public TaskResponse changeStatus(UUID taskId, TaskStatusRequest request) {
        Task task = findTask(taskId);
        checkVersion(task, request.version());
        if (!isValidTransition(task.getStatus(), request.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid task status transition: " + task.getStatus() + " -> " + request.status());
        }
        if (request.status() == TaskStatus.IN_PROGRESS && task.getDependencies().stream()
                .anyMatch(dependency -> dependency.getStatus() != TaskStatus.DONE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete task dependencies before starting this task");
        }
        TaskStatus previous = task.getStatus();
        task.updateStatus(request.status());
        tasks.flush();
        history.save(new TaskStatusHistory(task.getId(), previous, request.status()));
        return TaskResponse.from(task);
    }

    public TaskResponse updateDependencies(UUID taskId, TaskDependenciesRequest request) {
        Task task = findTask(taskId);
        checkVersion(task, request.version());
        Set<Task> dependencies = loadDependencies(task.getProject().getId(), request.dependencyIds());
        if (task.getStatus() != TaskStatus.TODO && dependencies.stream()
                .anyMatch(dependency -> dependency.getStatus() != TaskStatus.DONE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Started tasks can only depend on completed tasks");
        }
        for (Task dependency : dependencies) {
            if (dependsOn(dependency, task.getId(), new HashSet<>())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Task dependencies cannot contain a cycle");
            }
        }
        task.updateDependencies(dependencies);
        tasks.flush();
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> history(UUID taskId) {
        if (!tasks.existsById(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return history.findByTaskIdOrderByChangedAtAscIdAsc(taskId).stream()
                .map(TaskHistoryResponse::from).toList();
    }

    private Task findTask(UUID id) {
        return tasks.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private Set<Task> loadDependencies(UUID projectId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        List<Task> found = tasks.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more task dependencies do not exist");
        }
        if (found.stream().anyMatch(task -> !task.getProject().getId().equals(projectId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task dependencies must belong to the same project");
        }
        return new HashSet<>(found);
    }

    private boolean dependsOn(Task current, UUID target, Set<UUID> visited) {
        if (current.getId().equals(target)) return true;
        if (!visited.add(current.getId())) return false;
        return current.getDependencies().stream().anyMatch(dependency -> dependsOn(dependency, target, visited));
    }

    private void checkVersion(Task task, long expectedVersion) {
        if (task.getVersion() != expectedVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was changed by another request; reload and retry");
        }
    }

    private boolean isValidTransition(TaskStatus current, TaskStatus next) {
        return switch (current) {
            case TODO -> next == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TaskStatus.TODO || next == TaskStatus.DONE;
            case DONE -> next == TaskStatus.IN_PROGRESS;
        };
    }
}
