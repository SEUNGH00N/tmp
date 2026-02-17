CREATE TABLE workspace (
    id UUID PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    owner_account_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_workspace_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE TABLE workspace_account (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE TABLE workspace_membership (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES workspace_account(id) ON DELETE CASCADE,
    role_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_workspace_membership UNIQUE (workspace_id, account_id),
    CONSTRAINT chk_workspace_membership_role CHECK (role_code IN ('OWNER', 'MEMBER')),
    CONSTRAINT chk_workspace_membership_status CHECK (status IN ('ACTIVE', 'INVITED', 'REMOVED'))
);

ALTER TABLE import_job
    ADD COLUMN IF NOT EXISTS workspace_id UUID;

INSERT INTO workspace (id, slug, name, status, owner_account_id, created_at, updated_at)
SELECT DISTINCT
    j.tenant_id,
    'ws-' || REPLACE(CAST(j.tenant_id AS VARCHAR), '-', ''),
    'Workspace ' || SUBSTRING(CAST(j.tenant_id AS VARCHAR), 1, 8),
    'ACTIVE',
    j.tenant_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM import_job j
WHERE j.tenant_id IS NOT NULL;

INSERT INTO workspace_account (id, email, password_hash, display_name, active, created_at)
SELECT DISTINCT
    j.tenant_id,
    'owner+' || REPLACE(CAST(j.tenant_id AS VARCHAR), '-', '') || '@workspace.local',
    'changeme',
    'Workspace Owner',
    TRUE,
    CURRENT_TIMESTAMP
FROM import_job j
WHERE j.tenant_id IS NOT NULL;

INSERT INTO workspace_membership (id, workspace_id, account_id, role_code, status, joined_at)
SELECT DISTINCT
    j.tenant_id,
    j.tenant_id,
    j.tenant_id,
    'OWNER',
    'ACTIVE',
    CURRENT_TIMESTAMP
FROM import_job j
WHERE j.tenant_id IS NOT NULL;

UPDATE import_job
SET workspace_id = tenant_id
WHERE workspace_id IS NULL;

ALTER TABLE import_job
    ALTER COLUMN workspace_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_import_job_workspace_created ON import_job(workspace_id, created_at DESC);
