-- ERD-based improvements and seed data
CREATE INDEX IF NOT EXISTS idx_excel_data_job_created ON excel_data(job_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_import_job_created ON import_job(created_at DESC);

INSERT INTO app_user (id, username, password, display_name, active, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000002', 'operator', 'operator1234', 'Data Operator', TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', 'viewer', 'viewer1234', 'Read Only User', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

INSERT INTO import_job (
    id, tenant_id, status, file_uri, total_rows, processed_rows, success_count, fail_count,
    error_log_uri, created_at, started_at, finished_at
)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'COMPLETED'::import_job_status,
        '/storage/20000000-0000-0000-0000-000000000001/abc12345/file1.csv',
        3, 3, 3, 0,
        NULL,
        CURRENT_TIMESTAMP - INTERVAL '2 day',
        CURRENT_TIMESTAMP - INTERVAL '2 day',
        CURRENT_TIMESTAMP - INTERVAL '2 day' + INTERVAL '5 minute'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        'FAILED'::import_job_status,
        '/storage/20000000-0000-0000-0000-000000000002/def67890/file2.xlsx',
        4, 4, 2, 2,
        '/storage/errors/job2-errors.csv',
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '7 minute'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO excel_data (id, job_id, payload_json, created_at)
VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        '{"col_1":"A100","col_2":"Alice","col_3":"alice@example.com"}'::jsonb,
        CURRENT_TIMESTAMP - INTERVAL '2 day'
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        '{"col_1":"A101","col_2":"Bob","col_3":"bob@example.com"}'::jsonb,
        CURRENT_TIMESTAMP - INTERVAL '2 day' + INTERVAL '1 minute'
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000001',
        '{"col_1":"A102","col_2":"Carol","col_3":"carol@example.com"}'::jsonb,
        CURRENT_TIMESTAMP - INTERVAL '2 day' + INTERVAL '2 minute'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO error_log (id, job_id, row_index, column_name, error_code, error_msg, created_at)
VALUES
    (
        '40000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000002',
        2,
        'col_1',
        'IMP-VAL-001',
        'col_1 is required',
        CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '2 minute'
    ),
    (
        '40000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000002',
        4,
        'col_3',
        'IMP-VAL-002',
        'cell length must be <= 255',
        CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '3 minute'
    )
ON CONFLICT (id) DO NOTHING;
