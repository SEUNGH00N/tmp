-- 1.1.2 status strategy: keep VARCHAR + enforce allowed values via CHECK constraint
ALTER TABLE import_job
    ALTER COLUMN status TYPE VARCHAR(32)
    USING status::text;

UPDATE import_job
SET status = UPPER(status)
WHERE status IS NOT NULL;

ALTER TABLE import_job
    DROP CONSTRAINT IF EXISTS chk_import_job_status;

ALTER TABLE import_job
    ADD CONSTRAINT chk_import_job_status
        CHECK (status IN ('CREATED', 'PARSING', 'VALIDATING', 'LOADING', 'COMPLETED', 'FAILED'));

-- 1.1.3 index hygiene
CREATE INDEX IF NOT EXISTS idx_import_job_created ON import_job(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_error_log_job_created ON error_log(job_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_excel_data_job_created ON excel_data(job_id, created_at DESC);
