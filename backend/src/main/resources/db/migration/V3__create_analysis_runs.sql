CREATE TABLE office.analysis_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES office.projects(id),
    demand VARCHAR(5000) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    artifact_json TEXT,
    artifact_hash CHAR(64),
    model VARCHAR(120),
    context_version VARCHAR(80) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT analysis_run_status_valid CHECK (status IN (
        'QUEUED', 'RUNNING', 'AWAITING_APPROVAL', 'APPROVED', 'REJECTED',
        'FAILED', 'CANCEL_REQUESTED', 'CANCELLED', 'EXPIRED'
    )),
    CONSTRAINT analysis_run_idempotency_unique UNIQUE (project_id, idempotency_key)
);

CREATE INDEX analysis_runs_project_created_idx ON office.analysis_runs (project_id, created_at DESC, id DESC);

CREATE TABLE office.analysis_attempts (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES office.analysis_runs(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    model VARCHAR(120) NOT NULL,
    context_version VARCHAR(80) NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    error_code VARCHAR(80),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT analysis_attempt_status_valid CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT analysis_attempt_number_positive CHECK (attempt_number > 0),
    CONSTRAINT analysis_attempt_unique UNIQUE (run_id, attempt_number)
);

CREATE TABLE office.analysis_run_events (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES office.analysis_runs(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    actor VARCHAR(120) NOT NULL,
    reason VARCHAR(80) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX analysis_run_events_run_time_idx ON office.analysis_run_events (run_id, occurred_at, id);

CREATE TABLE office.analysis_approvals (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL UNIQUE REFERENCES office.analysis_runs(id),
    decision VARCHAR(20) NOT NULL,
    artifact_hash CHAR(64) NOT NULL,
    decided_by VARCHAR(120) NOT NULL,
    created_task_ids TEXT NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT analysis_approval_decision_valid CHECK (decision IN ('APPROVED', 'REJECTED'))
);
