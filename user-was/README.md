# User WAS (separate from admin WAS)

Separate Spring Boot app for end-users.

- Port: `8081`
- Front pages:
  - `GET /` -> `/user/login.html`
  - `GET /user/login.html`
  - `GET /user/dashboard.html`
  - `GET /user/job-detail.html?jobId=...`

## Run
```powershell
cd .\user-was
mvn spring-boot:run
```

## API
- `POST /api/v1/user/imports` (multipart: `tenant_id`, `file`)
- `GET /api/v1/user/imports?tenant_id=...&page=0&size=20`
- `GET /api/v1/user/imports/{jobId}?tenant_id=...`
- `GET /api/v1/user/imports/{jobId}/errors?tenant_id=...&page=0&size=50`

## Implementation scope (start)
- Upload file and create `CREATED` job in `import_job`
- List user tenant jobs and check job status
- Show validation errors for a job
- Vue 3 CDN-based user UI (tenant input + upload + status pages)

## Notes
- Shares PostgreSQL schema (`import_job`, `error_log`) with admin WAS.
- File storage path follows the checksum-based local storage rule.
- Async processing dispatch integration is intentionally left as next step.
