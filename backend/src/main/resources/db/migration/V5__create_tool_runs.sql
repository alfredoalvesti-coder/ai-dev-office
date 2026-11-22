CREATE TABLE office.tool_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES office.projects(id),
    tool VARCHAR(20) NOT NULL CHECK (tool IN ('NODE_CHECK', 'NODE_RUN')),
    source TEXT NOT NULL,
    review_hash VARCHAR(64) NOT NULL,
    image VARCHAR(255),
    status VARCHAR(24) NOT NULL CHECK (status IN (
        'AWAITING_APPROVAL','QUEUED','RUNNING','CANCEL_REQUESTED','CANCELLED',
        'SUCCEEDED','FAILED','TIMED_OUT','EXPIRED'
    )),
    requested_by VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120),
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    lease_until TIMESTAMP WITH TIME ZONE,
    exit_code INTEGER,
    output TEXT,
    output_truncated BOOLEAN NOT NULL DEFAULT FALSE,
    error_code VARCHAR(40),
    elapsed_ms BIGINT,
    CHECK ((approved_by IS NULL) = (approved_at IS NULL))
);

CREATE INDEX tool_runs_project_history_idx ON office.tool_runs(project_id, created_at DESC, id DESC);
CREATE INDEX tool_runs_dispatch_idx ON office.tool_runs(status, updated_at);
