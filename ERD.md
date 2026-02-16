# ERD (As-Is / To-Be)

This document separates the current database model (As-Is) and the planned additions (To-Be).

## 1. As-Is (Currently Implemented)

### 1.1 Status
- Migration baseline: `V1` ~ `V5`
- `import_job.status` is now `VARCHAR(32)` with a CHECK constraint

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

  IMPORT_JOB ||--o{ EXCEL_DATA : has_rows
  IMPORT_JOB ||--o{ ERROR_LOG : has_errors
```

### 1.3 Indexes (Current)
- `idx_import_job_created` on `import_job(created_at DESC)`
- `idx_excel_data_job_created` on `excel_data(job_id, created_at DESC)`
- `idx_error_log_job_created` on `error_log(job_id, created_at DESC)`

---

## 2. To-Be (Planned Additions)

> The model below is proposed from:
> - `admin-was/docs/ADMIN_ENHANCEMENT_PLAN.md`
> - `user-was/docs/USER_ENHANCEMENT_PLAN.md`
> and is not implemented yet.

### 2.1 Authorization and User Management Extension (Admin)

```mermaid
erDiagram
  APP_USER ||--o{ USER_ROLE : has
  ROLE ||--o{ USER_ROLE : assigned_to
  ROLE ||--o{ ROLE_PERMISSION : grants
  PERMISSION ||--o{ ROLE_PERMISSION : included_in

  APP_USER {
    UUID id PK
    VARCHAR username UK
    VARCHAR password
    VARCHAR display_name
    BOOLEAN active
    TIMESTAMP created_at
    TIMESTAMP password_changed_at
    BOOLEAN must_change_password
    INT failed_login_count
    TIMESTAMP locked_until
    TIMESTAMP last_login_at
  }

  ROLE {
    UUID id PK
    VARCHAR name UK
    VARCHAR description
    BOOLEAN active
    TIMESTAMP created_at
  }

  PERMISSION {
    UUID id PK
    VARCHAR resource
    VARCHAR action
    VARCHAR description
  }

  USER_ROLE {
    UUID user_id FK
    UUID role_id FK
    UUID assigned_by
    TIMESTAMP created_at
  }

  ROLE_PERMISSION {
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
```

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
1. Authorization model (`role`, `permission`, `user_role`, `role_permission`) + `audit_log`
2. Extend `import_job` (`original_filename`, `file_size`, `checksum`)
3. Add event/metric tables (`import_job_event`, `import_job_metric`)
4. Add dedup policy table (`import_dedup`) and retry policy

---

## 3. Change Management Rules
- Apply To-Be tables with new Flyway migrations (`V6+`) in phases
- Keep backward compatibility for existing APIs
- Run migration rehearsal with sample data before production rollout