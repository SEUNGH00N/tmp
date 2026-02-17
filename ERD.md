# ERD (As-Is / To-Be)

This document separates the current database model (As-Is) and the planned additions (To-Be).

## 1. As-Is (Currently Implemented)

### 1.1 Status
- Migration baseline: `V1` ~ `V6`
- `import_job.status` is `VARCHAR(32)` with a CHECK constraint
- Admin RBAC schema is implemented in `V6`

### 1.2 ERD (Current)
```mermaid
erDiagram
  APP_USER {
    UUID id PK
    VARCHAR username UK
    VARCHAR password
    VARCHAR display_name
    BOOLEAN active
    TIMESTAMP created_at
  }

  APP_ROLE {
    UUID id PK
    VARCHAR code UK
    VARCHAR name
    TEXT description
    BOOLEAN active
    TIMESTAMP created_at
  }

  APP_PERMISSION {
    UUID id PK
    VARCHAR code UK
    VARCHAR resource
    VARCHAR action
    TEXT description
    TIMESTAMP created_at
  }

  APP_USER_ROLE {
    UUID id PK
    UUID user_id FK
    UUID role_id FK
    UUID assigned_by FK
    TIMESTAMP created_at
  }

  APP_ROLE_PERMISSION {
    UUID id PK
    UUID role_id FK
    UUID permission_id FK
    TIMESTAMP created_at
  }

  AUDIT_LOG {
    UUID id PK
    VARCHAR actor
    VARCHAR target_type
    UUID target_id
    VARCHAR action
    JSONB before_json
    JSONB after_json
    TIMESTAMP created_at
  }

  IMPORT_JOB {
    UUID id PK
    UUID tenant_id
    VARCHAR status
    TEXT file_uri
    INT total_rows
    INT processed_rows
    INT success_count
    INT fail_count
    TEXT error_log_uri
    TIMESTAMP created_at
    TIMESTAMP started_at
    TIMESTAMP finished_at
  }

  EXCEL_DATA {
    UUID id PK
    UUID job_id FK
    JSONB payload_json
    TIMESTAMP created_at
  }

  ERROR_LOG {
    UUID id PK
    UUID job_id FK
    INT row_index
    VARCHAR column_name
    VARCHAR error_code
    TEXT error_msg
    TIMESTAMP created_at
  }

  APP_USER ||--o{ APP_USER_ROLE : has_roles
  APP_ROLE ||--o{ APP_USER_ROLE : assigned_to_users
  APP_ROLE ||--o{ APP_ROLE_PERMISSION : grants
  APP_PERMISSION ||--o{ APP_ROLE_PERMISSION : included_in

  IMPORT_JOB ||--o{ EXCEL_DATA : has_rows
  IMPORT_JOB ||--o{ ERROR_LOG : has_errors
```

### 1.3 Indexes (Current)
- `idx_import_job_created` on `import_job(created_at DESC)`
- `idx_excel_data_job_created` on `excel_data(job_id, created_at DESC)`
- `idx_error_log_job_created` on `error_log(job_id, created_at DESC)`
- `idx_app_user_role_user` on `app_user_role(user_id)`
- `idx_app_user_role_role` on `app_user_role(role_id)`
- `idx_audit_log_target_created` on `audit_log(target_type, target_id, created_at DESC)`

---

## 2. To-Be (Planned Additions)

### 2.1 Admin Account Hardening (Next)
- Extend `app_user` with security lifecycle fields:
  - `password_changed_at`
  - `must_change_password`
  - `failed_login_count`
  - `locked_until`
  - `last_login_at`

### 2.2 Upload and Processing Extension (User)
```mermaid
erDiagram
  IMPORT_JOB ||--o{ IMPORT_JOB_EVENT : has_events
  IMPORT_JOB ||--o{ IMPORT_JOB_METRIC : has_metrics

  IMPORT_JOB {
    UUID id PK
    UUID tenant_id
    VARCHAR status
    TEXT file_uri
    VARCHAR original_filename
    BIGINT file_size
    VARCHAR checksum
    JSONB validation_summary_json
    INT total_rows
    INT processed_rows
    INT success_count
    INT fail_count
    TIMESTAMP created_at
    TIMESTAMP started_at
    TIMESTAMP finished_at
  }

  IMPORT_JOB_EVENT {
    UUID id PK
    UUID job_id FK
    VARCHAR event_type
    VARCHAR event_status
    JSONB payload_json
    TIMESTAMP created_at
  }

  IMPORT_JOB_METRIC {
    UUID id PK
    UUID job_id FK
    VARCHAR metric_key
    DOUBLE metric_value
    TIMESTAMP created_at
  }

  IMPORT_DEDUP {
    UUID id PK
    UUID tenant_id
    VARCHAR checksum
    UUID latest_job_id
    TIMESTAMP created_at
  }
```

### 2.3 Recommended Rollout Order
1. `app_user` security lifecycle columns
2. `import_job` metadata columns (`original_filename`, `file_size`, `checksum`)
3. event/metric tables (`import_job_event`, `import_job_metric`)
4. dedup policy table (`import_dedup`) and retry policy

---

## 3. Change Management Rules
- Apply To-Be tables with new Flyway migrations (`V7+`) in phases
- Keep backward compatibility for existing APIs
- Run migration rehearsal with sample data before production rollout
