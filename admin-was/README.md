# Admin WAS

## Scope
Admin-facing Spring Boot application.

- Session login/logout (`/api/v1/auth/*`)
- Import management APIs (`/api/v1/imports*`)
- Users/settings APIs
- Flyway migration owner
- Admin frontend pages under `/mock/*`

## Runtime
- Port: `8080`
- Base URL: `http://localhost:8080`

## Run
```powershell
cd .\admin-was
mvn spring-boot:run
```

## API
### Auth
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

### Import
- `POST /api/v1/imports` (`multipart`: `file`, `tenant_id`)
- `GET /api/v1/imports`
- `GET /api/v1/imports/{jobId}`
- `GET /api/v1/imports/{jobId}/rows?page=0&size=50`
- `GET /api/v1/imports/{jobId}/errors?page=0&size=50`

### Operations
- `GET /api/v1/users?page=0&size=20`
- `GET /api/v1/settings`
- `PUT /api/v1/settings`

## Front Pages
- `GET /` -> `/login.html`
- `GET /login.html`
- `GET /mock/dashboard.html`
- `GET /mock/job-list.html`
- `GET /mock/job-details.html`
- `GET /mock/settings.html`

## Data and Migration
- Uses PostgreSQL `exceldb`
- Flyway enabled in this module
- Shared tables: `app_user`, `import_job`, `excel_data`, `error_log`

## Notes
- Session store is Redis (`spring.session.store-type=redis`).
- Request IDs are propagated in API response metadata.