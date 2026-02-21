CREATE TABLE db_connection (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    db_type VARCHAR(32) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    db_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    secret_ref VARCHAR(500),
    ssl_mode VARCHAR(32) NOT NULL DEFAULT 'disable',
    password_enc TEXT,
    key_version INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_db_connection_workspace_name UNIQUE (workspace_id, name),
    CONSTRAINT chk_db_connection_db_type CHECK (db_type IN ('POSTGRESQL')),
    CONSTRAINT chk_db_connection_ssl_mode CHECK (ssl_mode IN ('disable', 'allow', 'prefer', 'require', 'verify-ca', 'verify-full'))
);

CREATE INDEX idx_db_connection_workspace_created
    ON db_connection(workspace_id, created_at DESC);
