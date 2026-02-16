# User WAS (separate from admin WAS)

This is a separate Spring Boot app for end-users.

- Port: `8081`
- Base API: `/api/v1/user/imports`

## Run
```powershell
cd .\user-was
mvn spring-boot:run
```

## APIs
- `POST /api/v1/user/imports` (multipart: `tenant_id`, `file`)
- `GET /api/v1/user/imports?tenant_id=...&page=0&size=20`
- `GET /api/v1/user/imports/{jobId}?tenant_id=...`

## Notes
- Uses the same PostgreSQL schema (`import_job`) as admin WAS.
- This app creates jobs in `CREATED` status and stores files.
- Processing dispatch is left as a skeleton integration point.
