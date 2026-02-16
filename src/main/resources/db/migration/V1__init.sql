CREATE TYPE import_job_status AS ENUM ('CREATED', 'PARSING', 'VALIDATING', 'LOADING', 'COMPLETED', 'FAILED');

CREATE TABLE import_job (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    status import_job_status NOT NULL,
    file_uri TEXT NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    processed_rows INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    error_log_uri TEXT,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE TABLE excel_data (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_job(id),
    payload_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE error_log (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_job(id),
    row_index INT NOT NULL,
    column_name VARCHAR(255) NOT NULL,
    error_code VARCHAR(255) NOT NULL,
    error_msg TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_error_log_job_created ON error_log(job_id, created_at DESC);
