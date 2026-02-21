CREATE TABLE approval_request (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    job_id UUID REFERENCES import_job(id) ON DELETE SET NULL,
    request_type VARCHAR(100) NOT NULL,
    policy_mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_approval_request_policy_mode CHECK (policy_mode IN ('OWNER', 'PLATFORM_ADMIN', 'DUAL_APPROVAL')),
    CONSTRAINT chk_approval_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELED'))
);

CREATE TABLE approval_decision (
    id UUID PRIMARY KEY,
    approval_request_id UUID NOT NULL REFERENCES approval_request(id) ON DELETE CASCADE,
    decided_by VARCHAR(100) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    reason TEXT,
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_approval_decision_decision CHECK (decision IN ('APPROVE', 'REJECT'))
);

CREATE TABLE workspace_feature_flag (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    feature_code VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    config_json JSONB,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_workspace_feature_flag UNIQUE (workspace_id, feature_code)
);

CREATE INDEX idx_approval_request_workspace_created
    ON approval_request(workspace_id, created_at DESC);

CREATE INDEX idx_approval_request_status_created
    ON approval_request(status, created_at DESC);

CREATE INDEX idx_approval_decision_request_created
    ON approval_decision(approval_request_id, created_at DESC);
