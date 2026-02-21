CREATE TABLE billing_plan (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    monthly_upload_quota INT NOT NULL,
    max_file_size_mb BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plan_feature (
    id UUID PRIMARY KEY,
    billing_plan_id UUID NOT NULL REFERENCES billing_plan(id) ON DELETE CASCADE,
    feature_code VARCHAR(100) NOT NULL,
    limit_unit VARCHAR(50),
    limit_int INT,
    limit_bigint BIGINT,
    limit_bool BOOLEAN,
    limit_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plan_feature UNIQUE (billing_plan_id, feature_code)
);

CREATE TABLE workspace_subscription (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    billing_plan_id UUID NOT NULL REFERENCES billing_plan(id),
    status VARCHAR(32) NOT NULL,
    current_flag BOOLEAN NOT NULL DEFAULT TRUE,
    billing_cycle_anchor TIMESTAMP,
    renewed_at TIMESTAMP,
    canceled_at TIMESTAMP,
    provider VARCHAR(50),
    external_subscription_id VARCHAR(255),
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_workspace_subscription_status CHECK (status IN ('ACTIVE', 'CANCELED', 'PAST_DUE', 'TRIALING'))
);

CREATE TABLE workspace_usage_daily (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    usage_day DATE NOT NULL,
    uploaded_files INT NOT NULL DEFAULT 0,
    processed_rows BIGINT NOT NULL DEFAULT 0,
    failed_rows BIGINT NOT NULL DEFAULT 0,
    storage_bytes BIGINT NOT NULL DEFAULT 0,
    job_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_workspace_usage_daily UNIQUE (workspace_id, usage_day)
);

CREATE INDEX idx_workspace_subscription_workspace_started
    ON workspace_subscription(workspace_id, started_at DESC);

CREATE INDEX idx_workspace_usage_daily_workspace_day
    ON workspace_usage_daily(workspace_id, usage_day DESC);

INSERT INTO billing_plan (id, code, name, monthly_upload_quota, max_file_size_mb, active, created_at) VALUES
('a1000000-0000-0000-0000-000000000001', 'FREE', 'Free Plan', 100, 20, TRUE, CURRENT_TIMESTAMP),
('a1000000-0000-0000-0000-000000000002', 'PRO', 'Pro Plan', 5000, 100, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO plan_feature (id, billing_plan_id, feature_code, limit_unit, limit_int, created_at) VALUES
('a2000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'MONTHLY_UPLOADS', 'files', 100, CURRENT_TIMESTAMP),
('a2000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001', 'MAX_FILE_SIZE_MB', 'mb', 20, CURRENT_TIMESTAMP),
('a2000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000002', 'MONTHLY_UPLOADS', 'files', 5000, CURRENT_TIMESTAMP),
('a2000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000002', 'MAX_FILE_SIZE_MB', 'mb', 100, CURRENT_TIMESTAMP)
ON CONFLICT (billing_plan_id, feature_code) DO NOTHING;

INSERT INTO workspace_subscription (
    id,
    workspace_id,
    billing_plan_id,
    status,
    current_flag,
    billing_cycle_anchor,
    renewed_at,
    canceled_at,
    provider,
    external_subscription_id,
    started_at,
    ended_at,
    created_at,
    updated_at
)
SELECT
    w.id,
    w.id,
    'a1000000-0000-0000-0000-000000000001',
    'ACTIVE',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL,
    'LOCAL',
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM workspace w
WHERE NOT EXISTS (
    SELECT 1 FROM workspace_subscription s WHERE s.workspace_id = w.id AND s.current_flag = TRUE
);
