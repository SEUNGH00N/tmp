# Next Sprint Checklist (Layer-Separated)

## 0. Rules
- Work by layer: `common -> admin-was -> user-was`.
- Keep API compatibility unless explicitly versioned.
- Every completed item must include: code, test, docs update.

---

## 1. Common Layer (`platform-common`)

### 1.1 Architecture
- [ ] Finalize package boundaries: `common.web`, `common.error`, `common.security`.
- [ ] Remove remaining duplicated cross-cutting classes from WAS modules.
- [ ] Add shared coding conventions doc (`common` README section).

### 1.2 API Contract
- [ ] Standardize response envelope and metadata fields across admin/user.
- [ ] Standardize error model (RFC7807 + internal error code).
- [ ] Add request-id propagation guideline for logs and responses.

### 1.3 Quality
- [ ] Add unit tests for common utilities.
- [ ] Add module-level CI check for dependency drift.

---

## 2. Admin WAS (`admin-was`)

### 2.1 Identity / RBAC
- [ ] Split admin account naming/domain from generic user naming.
- [ ] Add permission-level authorization checks (beyond role coarse checks).
- [ ] Implement role/permission management API for CRUD operations.

### 2.2 Admin Operations
- [ ] Add audit log query API (`/api/v1/admin/audit-logs`) with filters.
- [ ] Add admin approval queue API for schema/DDL requests.
- [ ] Add approve/reject decision API with reason and actor trace.

### 2.3 Admin UI
- [ ] Add audit log page (list/filter/detail).
- [ ] Add role-permission matrix editor page.
- [ ] Add approval inbox page (pending, approved, rejected).

### 2.4 Data / Migration
- [ ] Add next migration for admin account hardening fields.
- [ ] Add indexes for approval/audit high-cardinality queries.

---

## 3. User WAS (`user-was`)

### 3.1 User Identity
- [ ] Introduce `tenant_user` auth model (replace tenant-id-only login path).
- [ ] Add tenant/workspace membership validation on every user API.

### 3.2 Ingestion / Schema Draft
- [ ] Add schema draft generation after parsing/profiling.
- [ ] Add schema draft edit API (column type/nullability/key candidates).
- [ ] Add approval request submission flow for schema confirmation.

### 3.3 User UI
- [ ] Add schema preview/edit screen before DB apply.
- [ ] Add upload result summary and validation analytics.
- [ ] Add error export UX and retry action UX.

### 3.4 Robustness
- [ ] Add checksum dedup policy and duplicate upload behavior.
- [ ] Add resumable/retry-safe processing states.
- [ ] Add large-file strategy (stream/chunk) design + PoC.

---

## 4. Shared Workflow (Admin + User)

### 4.1 Approval
- [ ] Define `approval_mode` policy:
  - [ ] `SELF_APPROVAL`
  - [ ] `ADMIN_APPROVAL`
  - [ ] `DUAL_APPROVAL`
- [ ] Add organization/workspace-level policy storage and enforcement.

### 4.2 DDL / ERD
- [ ] Add DDL plan generation from approved schema version.
- [ ] Add controlled DDL execution records and rollback strategy.
- [ ] Add ERD export formats (`mermaid`, SQL DDL).

### 4.3 Observability
- [ ] Correlate logs with `requestId`, `jobId`, `tenantId`, `approvalId`.
- [ ] Add processing metrics and alert thresholds.

---

## 5. Definition of Done
- [ ] Code implemented and reviewed.
- [ ] `mvn test` passes at root.
- [ ] README / ERD / API docs updated.
- [ ] Manual test scenarios recorded.
