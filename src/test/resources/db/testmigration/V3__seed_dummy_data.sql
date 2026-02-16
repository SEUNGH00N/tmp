CREATE INDEX idx_excel_data_job_created ON excel_data(job_id, created_at DESC);
CREATE INDEX idx_import_job_created ON import_job(created_at DESC);

MERGE INTO app_user (id, username, password, display_name, active, created_at)
KEY(username)
VALUES
    ('00000000-0000-0000-0000-000000000002', 'operator', 'operator1234', 'Data Operator', TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', 'viewer', 'viewer1234', 'Read Only User', TRUE, CURRENT_TIMESTAMP);

INSERT INTO import_job (id, tenant_id, status, file_uri, total_rows, processed_rows, success_count, fail_count, error_log_uri, created_at, started_at, finished_at)
SELECT '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'COMPLETED', '/storage/tenant1/file1.csv', 3, 3, 3, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM import_job WHERE id='10000000-0000-0000-0000-000000000001');

INSERT INTO import_job (id, tenant_id, status, file_uri, total_rows, processed_rows, success_count, fail_count, error_log_uri, created_at, started_at, finished_at)
SELECT '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'FAILED', '/storage/tenant2/file2.xlsx', 4, 4, 2, 2, '/storage/errors/job2-errors.csv', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM import_job WHERE id='10000000-0000-0000-0000-000000000002');

INSERT INTO excel_data (id, job_id, payload_json, created_at)
SELECT '30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '{"col_1":"A100","col_2":"Alice","col_3":"alice@example.com"}', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM excel_data WHERE id='30000000-0000-0000-0000-000000000001');

INSERT INTO excel_data (id, job_id, payload_json, created_at)
SELECT '30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '{"col_1":"A101","col_2":"Bob","col_3":"bob@example.com"}', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM excel_data WHERE id='30000000-0000-0000-0000-000000000002');

INSERT INTO excel_data (id, job_id, payload_json, created_at)
SELECT '30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '{"col_1":"A102","col_2":"Carol","col_3":"carol@example.com"}', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM excel_data WHERE id='30000000-0000-0000-0000-000000000003');

INSERT INTO error_log (id, job_id, row_index, column_name, error_code, error_msg, created_at)
SELECT '40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 2, 'col_1', 'IMP-VAL-001', 'col_1 is required', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM error_log WHERE id='40000000-0000-0000-0000-000000000001');

INSERT INTO error_log (id, job_id, row_index, column_name, error_code, error_msg, created_at)
SELECT '40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 4, 'col_3', 'IMP-VAL-002', 'cell length must be <= 255', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM error_log WHERE id='40000000-0000-0000-0000-000000000002');
