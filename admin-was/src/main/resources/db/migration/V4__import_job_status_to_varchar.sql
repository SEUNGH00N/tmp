ALTER TABLE import_job
    ALTER COLUMN status TYPE VARCHAR(32)
    USING status::text;

DROP TYPE IF EXISTS import_job_status;
