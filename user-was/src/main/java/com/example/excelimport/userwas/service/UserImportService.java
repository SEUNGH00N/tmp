package com.example.excelimport.userwas.service;

import com.example.excelimport.userwas.dto.UserImportCreateResponse;
import com.example.excelimport.userwas.dto.UserImportErrorItem;
import com.example.excelimport.userwas.dto.UserImportErrorListResponse;
import com.example.excelimport.userwas.dto.UserImportListItem;
import com.example.excelimport.userwas.dto.UserImportListResponse;
import com.example.excelimport.userwas.dto.UserImportRowItem;
import com.example.excelimport.userwas.dto.UserImportRowListResponse;
import com.example.excelimport.userwas.dto.UserImportStatusResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.storage.UserFileStorageService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserImportService {

    private final JdbcTemplate jdbcTemplate;
    private final UserFileStorageService storageService;
    private final UserImportEventPublisher userImportEventPublisher;
    private final UserBillingService userBillingService;
    private final UserGovernanceService userGovernanceService;

    public UserImportService(
            JdbcTemplate jdbcTemplate,
            UserFileStorageService storageService,
            UserImportEventPublisher userImportEventPublisher,
            UserBillingService userBillingService,
            UserGovernanceService userGovernanceService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.userImportEventPublisher = userImportEventPublisher;
        this.userBillingService = userBillingService;
        this.userGovernanceService = userGovernanceService;
    }

    public UserImportCreateResponse create(UUID workspaceId, MultipartFile file) {
        if (workspaceId == null) throw new UserWasException(400, "workspace_id is required");
        if (file == null || file.isEmpty()) throw new UserWasException(400, "file is required");

        ensureWorkspaceExists(workspaceId);
        userBillingService.validateUploadQuota(workspaceId, file.getSize());
        long fileSize = file.getSize();
        String checksum = sha256Hex(readBytes(file));

        UUID duplicateJobId = jdbcTemplate.query(
                "select latest_job_id from import_dedup where workspace_id = ? and checksum = ? and file_size = ?",
                rs -> rs.next() ? (UUID) rs.getObject("latest_job_id") : null,
                workspaceId, checksum, fileSize
        );
        if (duplicateJobId != null) {
            jdbcTemplate.update(
                    "insert into import_job_event (id, job_id, event_type, level, payload_json, created_at) " +
                            "values (?, ?, 'DEDUP_HIT', 'INFO', ?::jsonb, now())",
                    UUID.randomUUID(),
                    duplicateJobId,
                    "{\"source\":\"user\"}"
            );
            return new UserImportCreateResponse(duplicateJobId);
        }

        UserFileStorageService.StoredFile stored;
        try {
            stored = storageService.store(workspaceId, file);
        } catch (IOException e) {
            throw new UserWasException(500, e.getMessage());
        }

        UUID jobId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into import_job " +
                        "(id, tenant_id, workspace_id, status, file_uri, original_filename, file_size, checksum, total_rows, processed_rows, success_count, fail_count, error_log_uri, created_at, started_at, finished_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, null, now(), null, null)",
                jobId, workspaceId, workspaceId, "CREATED", stored.fileUri(), file.getOriginalFilename(), fileSize, checksum
        );
        userBillingService.addUploadAcceptedUsage(workspaceId, file.getSize());
        jdbcTemplate.update(
                "insert into import_dedup (id, workspace_id, checksum, file_size, latest_job_id, created_at, updated_at) " +
                        "values (?, ?, ?, ?, ?, now(), now()) " +
                        "on conflict (workspace_id, checksum, file_size) do update set latest_job_id = excluded.latest_job_id, updated_at = now()",
                UUID.randomUUID(), workspaceId, checksum, fileSize, jobId
        );

        Integer nextRunNo = jdbcTemplate.queryForObject(
                "select coalesce(max(run_no), 0) + 1 from import_job_run where job_id = ?",
                Integer.class,
                jobId
        );
        int runNo = nextRunNo == null ? 1 : nextRunNo;
        UUID runId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into import_job_run (id, job_id, run_no, worker_id, status, created_at, updated_at) " +
                        "values (?, ?, ?, ?, 'QUEUED', now(), now())",
                runId, jobId, runNo, "user-local"
        );

        boolean approvalRequired = userGovernanceService.isFeatureEnabled(workspaceId, "APPROVAL_REQUIRED");
        if (approvalRequired) {
            userGovernanceService.createImportApprovalRequest(
                    workspaceId,
                    jobId,
                    "workspace-user",
                    "PLATFORM_ADMIN",
                    "Approval required by feature flag"
            );
            jdbcTemplate.update(
                    "insert into import_job_event (id, job_id, event_type, level, payload_json, created_at) " +
                            "values (?, ?, 'APPROVAL_REQUIRED', 'INFO', ?::jsonb, now())",
                    UUID.randomUUID(), jobId, "{\"runNo\":" + runNo + "}"
            );
        } else {
            jdbcTemplate.update(
                    "insert into import_job_event (id, job_id, event_type, level, payload_json, created_at) " +
                            "values (?, ?, 'JOB_DISPATCHED', 'INFO', ?::jsonb, now())",
                    UUID.randomUUID(), jobId, "{\"runNo\":" + runNo + ",\"source\":\"user\"}"
            );

            userImportEventPublisher.publish(
                    new UserImportProcessRequestedEvent(jobId, runId, runNo, stored.extension(), stored.fileUri(), Instant.now())
            );
        }
        return new UserImportCreateResponse(jobId);
    }

    public UserImportListResponse list(UUID workspaceId, int page, int size) {
        if (workspaceId == null) throw new UserWasException(400, "workspace_id is required");
        if (size < 1) size = 20;
        if (page < 0) page = 0;

        Integer total = jdbcTemplate.queryForObject("select count(*) from import_job where workspace_id = ?", Integer.class, workspaceId);
        int totalElements = total == null ? 0 : total;

        List<UserImportListItem> items = jdbcTemplate.query(
                "select id, workspace_id, status, total_rows, processed_rows, created_at from import_job where workspace_id = ? order by created_at desc limit ? offset ?",
                new ImportListMapper(), workspaceId, size, page * size
        );

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new UserImportListResponse(items, page, size, totalElements, totalPages);
    }

    public UserImportStatusResponse status(UUID workspaceId, UUID jobId) {
        if (workspaceId == null) throw new UserWasException(400, "workspace_id is required");

        List<UserImportStatusResponse> rows = jdbcTemplate.query(
                "select id, status, total_rows, processed_rows from import_job where id = ? and workspace_id = ?",
                (rs, n) -> {
                    int totalRows = rs.getInt("total_rows");
                    int processedRows = rs.getInt("processed_rows");
                    int pct = totalRows == 0 ? 0 : (int) Math.floor((processedRows * 100.0) / totalRows);
                    return new UserImportStatusResponse(
                            (UUID) rs.getObject("id"),
                            rs.getString("status"),
                            pct,
                            totalRows,
                            processedRows
                    );
                },
                jobId, workspaceId
        );

        if (rows.isEmpty()) throw new UserWasException(404, "job not found");
        return rows.get(0);
    }

    public UserImportRowListResponse rows(UUID workspaceId, UUID jobId, int page, int size) {
        if (workspaceId == null) throw new UserWasException(400, "workspace_id is required");
        if (size < 1) size = 50;
        if (page < 0) page = 0;

        ensureJobOwned(workspaceId, jobId);

        Integer total = jdbcTemplate.queryForObject(
                "select count(*) from excel_data d join import_job j on d.job_id = j.id where d.job_id = ? and j.workspace_id = ?",
                Integer.class,
                jobId, workspaceId
        );
        int totalElements = total == null ? 0 : total;

        List<UserImportRowItem> items = jdbcTemplate.query(
                "select d.id, d.payload_json::text as payload_json, d.created_at " +
                        "from excel_data d join import_job j on d.job_id = j.id " +
                        "where d.job_id = ? and j.workspace_id = ? " +
                        "order by d.created_at desc limit ? offset ?",
                (rs, n) -> new UserImportRowItem(
                        (UUID) rs.getObject("id"),
                        rs.getString("payload_json"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                jobId, workspaceId, size, page * size
        );

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new UserImportRowListResponse(items, page, size, totalElements, totalPages);
    }

    public UserImportErrorListResponse errors(UUID workspaceId, UUID jobId, int page, int size) {
        if (workspaceId == null) throw new UserWasException(400, "workspace_id is required");
        if (size < 1) size = 50;
        if (page < 0) page = 0;

        ensureJobOwned(workspaceId, jobId);

        Integer total = jdbcTemplate.queryForObject(
                "select count(*) from error_log e join import_job j on e.job_id = j.id where e.job_id = ? and j.workspace_id = ?",
                Integer.class,
                jobId, workspaceId
        );
        int totalElements = total == null ? 0 : total;

        List<UserImportErrorItem> items = jdbcTemplate.query(
                "select e.row_index, e.column_name, e.error_code, e.error_msg, e.created_at " +
                        "from error_log e join import_job j on e.job_id = j.id " +
                        "where e.job_id = ? and j.workspace_id = ? " +
                        "order by e.created_at desc limit ? offset ?",
                (rs, n) -> new UserImportErrorItem(
                        rs.getInt("row_index"),
                        rs.getString("column_name"),
                        rs.getString("error_code"),
                        rs.getString("error_msg"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                jobId, workspaceId, size, page * size
        );

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new UserImportErrorListResponse(items, page, size, totalElements, totalPages);
    }

    private void ensureJobOwned(UUID workspaceId, UUID jobId) {
        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from import_job where id = ? and workspace_id = ?",
                Integer.class,
                jobId, workspaceId
        );
        if (exists == null || exists == 0) throw new UserWasException(404, "job not found");
    }

    private void ensureWorkspaceExists(UUID workspaceId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from workspace where id = ?", Integer.class, workspaceId);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                "insert into workspace (id, slug, name, status, owner_account_id, created_at, updated_at) values (?, ?, ?, 'ACTIVE', ?, now(), now())",
                workspaceId,
                "ws-" + workspaceId.toString().replace("-", ""),
                "Workspace " + workspaceId.toString().substring(0, 8),
                workspaceId
        );

        jdbcTemplate.update(
                "insert into workspace_account (id, email, password_hash, display_name, active, created_at) values (?, ?, ?, ?, true, now())",
                workspaceId,
                "owner+" + workspaceId.toString().replace("-", "") + "@workspace.local",
                "changeme",
                "Workspace Owner"
        );

        jdbcTemplate.update(
                "insert into workspace_membership (id, workspace_id, account_id, role_code, status, joined_at) values (?, ?, ?, 'OWNER', 'ACTIVE', now())",
                workspaceId,
                workspaceId,
                workspaceId
        );
    }

    private static class ImportListMapper implements RowMapper<UserImportListItem> {
        @Override
        public UserImportListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            int totalRows = rs.getInt("total_rows");
            int processedRows = rs.getInt("processed_rows");
            int pct = totalRows == 0 ? 0 : (int) Math.floor((processedRows * 100.0) / totalRows);
            Instant createdAt = rs.getTimestamp("created_at").toInstant();
            return new UserImportListItem(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("workspace_id"),
                    rs.getString("status"),
                    pct,
                    totalRows,
                    processedRows,
                    createdAt
            );
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new UserWasException(500, "failed to read file");
        }
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new UserWasException(500, "failed to compute checksum");
        }
    }
}
