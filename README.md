# Excel Import MVP

## Prerequisites
- Java 17
- Maven 3.9+
- Docker Desktop

## Run infra (PowerShell)
```powershell
docker compose -f .\docker-compose.yml up -d
```

## Run app (PowerShell)
```powershell
mvn spring-boot:run
```

## Test
```powershell
mvn test
```

## Login
- Access: `http://localhost:8080`
- Default credentials: `admin` / `admin1234`
- Credentials are seeded by Flyway migrations (`V2__auth_user.sql`, `V3__seed_dummy_data.sql`)

## API
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`
- `POST /api/v1/imports` (multipart: `file`, `tenant_id`)
- `GET /api/v1/imports` (paged)
- `GET /api/v1/imports/{jobId}`
- `GET /api/v1/imports/{jobId}/errors?page=0&size=50`
- `GET /api/v1/imports/{jobId}/rows?page=0&size=50`
- `GET /api/v1/users?page=0&size=20`
- `GET /api/v1/settings`
- `PUT /api/v1/settings`

## Seed data (V3)
- Users: `admin`, `operator`, `viewer`
- Sample jobs:
  - `10000000-0000-0000-0000-000000000001` (COMPLETED)
  - `10000000-0000-0000-0000-000000000002` (FAILED)

## Storage policy
- Storage root: `./storage` (configurable via `app.storage.root`)
- Final path: `{root}/{tenantId}/{sha256_prefix}/{sha256}.{ext}`
- TTL property: `app.storage.retention-days` (cleanup job optional)

## Frontend pages
- `GET /` -> redirects to login page
- `GET /login.html`
- `GET /mock/dashboard.html`
- `GET /mock/job-list.html`
- `GET /mock/job-details.html`
- `GET /mock/settings.html`

## Notes
- CSV/XLSX supported.
- Validation rules: `col_1` required, each cell max 255 chars.
- Partial success: invalid rows are logged to `error_log`, valid rows are saved.
- Original stitch screenshots are kept at `/mock/screens/*.png`.
