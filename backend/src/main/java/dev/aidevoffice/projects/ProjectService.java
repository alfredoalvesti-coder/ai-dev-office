package dev.aidevoffice.projects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projects;

    public ProjectService(ProjectRepository projects) {
        this.projects = projects;
    }

    public ProjectResponse create(ProjectRequest request) {
        return ProjectResponse.from(projects.save(new Project(request.name(), request.description())));
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(Pageable pageable) {
        return projects.findAll(pageable).map(ProjectResponse::from);
    }
}
