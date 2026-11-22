CREATE TABLE office.projects (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT projects_name_not_blank CHECK (length(trim(name)) > 0)
);

CREATE INDEX projects_created_at_id_idx ON office.projects (created_at DESC, id DESC);

CREATE TABLE office.tasks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES office.projects(id),
    title VARCHAR(180) NOT NULL,
    description VARCHAR(4000) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT tasks_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT tasks_status_valid CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))
);

CREATE INDEX tasks_project_created_at_idx ON office.tasks (project_id, created_at, id);

CREATE TABLE office.task_acceptance_criteria (
    task_id UUID NOT NULL REFERENCES office.tasks(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    criterion VARCHAR(500) NOT NULL,
    PRIMARY KEY (task_id, position)
);

CREATE TABLE office.task_dependencies (
    task_id UUID NOT NULL REFERENCES office.tasks(id) ON DELETE CASCADE,
    dependency_id UUID NOT NULL REFERENCES office.tasks(id),
    PRIMARY KEY (task_id, dependency_id),
    CONSTRAINT task_dependency_no_self CHECK (task_id <> dependency_id)
);

CREATE TABLE office.task_status_history (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES office.tasks(id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT task_history_status_valid CHECK (to_status IN ('TODO', 'IN_PROGRESS', 'DONE'))
);

CREATE INDEX task_status_history_task_time_idx ON office.task_status_history (task_id, changed_at, id);
