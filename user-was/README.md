# User WAS

## Scope
End-user Spring Boot application for tenant-scoped Excel upload and import monitoring.

- Upload CSV/XLSX
- Job list/status/detail (rows/errors)
- Async parsing/validation/loading pipeline
- User frontend pages under `/user/*`

## Runtime
- Port: `8081`
- Base URL: `http://localhost:8081`

## Run
```powershell
cd .\user-was
mvn spring-boot:run
```

## API
- `POST /api/v1/user/imports` (`multipart`: `tenant_id`, `file`)
- `GET /api/v1/user/imports?tenant_id=...&page=0&size=20`
- `GET /api/v1/user/imports/{jobId}?tenant_id=...`
- `GET /api/v1/user/imports/{jobId}/rows?tenant_id=...&page=0&size=50`
- `GET /api/v1/user/imports/{jobId}/errors?tenant_id=...&page=0&size=50`

## Front Pages
- `GET /` -> `/user/login.html`
- `GET /user/login.html`
- `GET /user/dashboard.html`
- `GET /user/job-detail.html?jobId=...`

## Current Processing Rules
- Status flow: `CREATED -> PARSING -> VALIDATING -> LOADING -> COMPLETED/FAILED`
- Validation:
  - `col_1` required (`IMP-VAL-001`)
  - max cell length 255 (`IMP-VAL-002`)
- Persistence:
  - valid rows -> `excel_data`
  - invalid rows -> `error_log`

## Data and Migration
- Shares same PostgreSQL schema as admin-was
- Flyway disabled in this module (schema managed by admin-was)
- Uses same storage policy: `{root}/{tenantId}/{sha256_prefix}/{sha256}.{ext}`

## Sample File
- `../upload_template_cases.xlsx`
  - `success_case`
  - `error_case`