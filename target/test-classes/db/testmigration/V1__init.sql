CREATE TABLE import_job (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    file_uri VARCHAR(1024) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    processed_rows INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    error_log_uri VARCHAR(1024),
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE TABLE excel_data (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    payload_json CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_excel_data_job FOREIGN KEY (job_id) REFERENCES import_job(id)
);

CREATE TABLE error_log (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    row_index INT NOT NULL,
    column_name VARCHAR(255) NOT NULL,
    error_code VARCHAR(255) NOT NULL,
    error_msg CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_error_log_job FOREIGN KEY (job_id) REFERENCES import_job(id)
);

CREATE INDEX idx_error_log_job_created ON error_log(job_id, created_at DESC);
