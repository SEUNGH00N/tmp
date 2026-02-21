ALTER TABLE import_job
    ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255),
    ADD COLUMN IF NOT EXISTS file_size BIGINT,
    ADD COLUMN IF NOT EXISTS checksum VARCHAR(128);

CREATE TABLE import_dedup (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    checksum VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    latest_job_id UUID NOT NULL REFERENCES import_job(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_import_dedup_workspace_checksum_size UNIQUE (workspace_id, checksum, file_size)
);

CREATE TABLE import_job_run (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_job(id) ON DELETE CASCADE,
    run_no INT NOT NULL,
    worker_id VARCHAR(100),
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_import_job_run_job_run_no UNIQUE (job_id, run_no),
    CONSTRAINT chk_import_job_run_status CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE import_job_event (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_job(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    level VARCHAR(32) NOT NULL,
    payload_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_import_job_event_level CHECK (level IN ('INFO', 'WARN', 'ERROR'))
);

CREATE TABLE import_job_metric (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_job(id) ON DELETE CASCADE,
    metric_key VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_import_dedup_workspace_created ON import_dedup(workspace_id, created_at DESC);
CREATE INDEX idx_import_job_run_job_created ON import_job_run(job_id, created_at DESC);
CREATE INDEX idx_import_job_event_job_created ON import_job_event(job_id, created_at DESC);
CREATE INDEX idx_import_job_metric_job_created ON import_job_metric(job_id, created_at DESC);
