package dev.aidevoffice.tasks;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable UUID projectId, @Valid @RequestBody TaskRequest request) {
        return service.create(projectId, request);
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public List<TaskResponse> list(@PathVariable UUID projectId) {
        return service.list(projectId);
    }

    @PutMapping("/api/tasks/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable UUID taskId, @Valid @RequestBody TaskStatusRequest request) {
        return service.changeStatus(taskId, request);
    }

    @PutMapping("/api/tasks/{taskId}/dependencies")
    public TaskResponse updateDependencies(@PathVariable UUID taskId,
                                           @Valid @RequestBody TaskDependenciesRequest request) {
        return service.updateDependencies(taskId, request);
    }

    @GetMapping("/api/tasks/{taskId}/history")
    public List<TaskHistoryResponse> history(@PathVariable UUID taskId) {
        return service.history(taskId);
    }
}
