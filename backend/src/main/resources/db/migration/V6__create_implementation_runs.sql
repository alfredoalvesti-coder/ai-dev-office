CREATE TABLE office.implementation_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES office.projects(id),
    team_run_id UUID NOT NULL REFERENCES office.team_runs(id),
    status VARCHAR(30) NOT NULL,
    artifact_json TEXT,
    artifact_hash CHAR(64),
    model VARCHAR(120),
    error_code VARCHAR(80),
    expires_at TIMESTAMP WITH TIME ZONE,
    workspace_relative VARCHAR(260),
    requested_by VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120),
    applied_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT implementation_status_valid CHECK (status IN (
        'QUEUED','RUNNING','AWAITING_APPROVAL','APPLIED','REJECTED','FAILED','CANCELLED','EXPIRED'
    ))
);

CREATE INDEX implementation_runs_project_idx ON office.implementation_runs(project_id, created_at DESC, id DESC);
CREATE INDEX implementation_runs_team_idx ON office.implementation_runs(team_run_id, created_at DESC);
CREATE INDEX implementation_runs_queue_idx ON office.implementation_runs(status, updated_at);
