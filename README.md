# Excel Import Platform

## Overview
Excel upload/import platform with separated Admin WAS and User WAS.

- `admin-was` manages auth, admin UI, settings, and schema migration.
- `user-was` provides tenant-scoped upload and import tracking APIs/UI.
- Both apps share PostgreSQL tables and local file storage policy.

## Project Layout
- `pom.xml`: root aggregator (`admin-was`, `user-was`)
- `docker-compose.yml`: infra only (`postgres`, `redis`)
- `admin-was/`: admin backend + admin frontend mock pages
- `user-was/`: user backend + user frontend pages
- `upload_template_cases.xlsx`: sample upload template (success/error cases)

## Runtime Ports
- Admin WAS: `http://localhost:8080`
- User WAS: `http://localhost:8081`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

## Prerequisites
- Java 17
- Maven 3.9+
- Docker Desktop

## Run
1. Infra
```powershell
docker compose -f .\docker-compose.yml up -d
```
2. Admin WAS
```powershell
cd .\admin-was
mvn spring-boot:run
```
3. User WAS
```powershell
cd .\user-was
mvn spring-boot:run
```

## Shared Data Model
- `app_user`: admin login users
- `import_job`: job header/status/counters
- `excel_data`: valid imported rows (`payload_json` JSONB)
- `error_log`: invalid row details

### Status Strategy
- `import_job.status` is `VARCHAR(32)` with CHECK constraint:
  - `CREATED`, `PARSING`, `VALIDATING`, `LOADING`, `COMPLETED`, `FAILED`

## Migration Ownership
- Flyway owner: `admin-was`
- Key migrations:
  - `V1__init.sql`
  - `V2__auth_user.sql`
  - `V3__seed_dummy_data.sql`
  - `V4__import_job_status_to_varchar.sql`
  - `V5__migration_hygiene.sql`

## MVP Status (Current)
- Multi-WAS separation done
- User upload -> async parse/validate/load flow implemented
- Valid rows persist to `excel_data`, invalid rows to `error_log`
- User job detail UI renders status + rows + errors
- Admin UI/API and user UI/API run independently on separate ports

## Backlog (High-Level)
- Role-based authorization model
- Schema inference/versioning for uploaded datasets
- Large-file performance tuning and streaming strategy
- Storage cleanup scheduler and operational health checks