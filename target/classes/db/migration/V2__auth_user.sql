CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

INSERT INTO app_user (id, username, password, display_name, active, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin1234',
    'Admin User',
    TRUE,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;
