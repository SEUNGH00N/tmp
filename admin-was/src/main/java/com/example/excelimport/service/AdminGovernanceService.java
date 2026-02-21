package com.example.excelimport.service;

import com.example.excelimport.dto.ApprovalRequestItem;
import com.example.excelimport.dto.FeatureFlagItem;
import com.example.excelimport.exception.ApiException;
import com.example.excelimport.upload.ImportJobDispatchPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminGovernanceService {

    private final JdbcTemplate jdbcTemplate;
    private final ImportJobDispatchPort importJobDispatchPort;

    public AdminGovernanceService(JdbcTemplate jdbcTemplate,
                                  ImportJobDispatchPort importJobDispatchPort) {
        this.jdbcTemplate = jdbcTemplate;
        this.importJobDispatchPort = importJobDispatchPort;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestItem> listApprovalRequests(String status) {
        if (status == null || status.isBlank()) {
            return jdbcTemplate.query(
                    "select id, workspace_id, job_id, request_type, policy_mode, status, requested_by, created_at " +
                            "from approval_request order by created_at desc limit 100",
                    (rs, n) -> new ApprovalRequestItem(
                            (UUID) rs.getObject("id"),
                            (UUID) rs.getObject("workspace_id"),
                            (UUID) rs.getObject("job_id"),
                            rs.getString("request_type"),
                            rs.getString("policy_mode"),
                            rs.getString("status"),
                            rs.getString("requested_by"),
                            rs.getTimestamp("created_at").toInstant()
                    )
            );
        }

        String normalized = status.trim().toUpperCase();
        return jdbcTemplate.query(
                "select id, workspace_id, job_id, request_type, policy_mode, status, requested_by, created_at " +
                        "from approval_request where status = ? order by created_at desc limit 100",
                (rs, n) -> new ApprovalRequestItem(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("workspace_id"),
                        (UUID) rs.getObject("job_id"),
                        rs.getString("request_type"),
                        rs.getString("policy_mode"),
                        rs.getString("status"),
                        rs.getString("requested_by"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                normalized
        );
    }

    @Transactional
    public void decideApproval(UUID requestId, String decision, String reason, String actor) {
        String normalizedDecision = normalizeDecision(decision);
        ApprovalRow request = fetchApprovalRequest(requestId);

        if (!"PENDING".equals(request.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/invalid-approval-state",
                    "Bad Request",
                    "approval request is not pending");
        }

        String nextStatus = "APPROVE".equals(normalizedDecision) ? "APPROVED" : "REJECTED";
        jdbcTemplate.update(
                "update approval_request set status = ?, updated_at = now() where id = ?",
                nextStatus, requestId
        );

        jdbcTemplate.update(
                "insert into approval_decision (id, approval_request_id, decided_by, decision, reason, decided_at, created_at) " +
                        "values (?, ?, ?, ?, ?, now(), now())",
                UUID.randomUUID(), requestId, actor, normalizedDecision, reason
        );

        if ("APPROVE".equals(normalizedDecision)
                && request.jobId() != null
                && "IMPORT_EXECUTION".equals(request.requestType())) {
            triggerJobDispatchIfReady(request.jobId());
        }
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagItem> listFeatureFlags(UUID workspaceId) {
        return jdbcTemplate.query(
                "select id, workspace_id, feature_code, enabled, config_json::text as config_json, updated_at " +
                        "from workspace_feature_flag where workspace_id = ? order by feature_code",
                (rs, n) -> new FeatureFlagItem(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("workspace_id"),
                        rs.getString("feature_code"),
                        rs.getBoolean("enabled"),
                        rs.getString("config_json"),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                workspaceId
        );
    }

    @Transactional
    public FeatureFlagItem upsertFeatureFlag(UUID workspaceId, String featureCode, boolean enabled, String configJson) {
        String normalizedCode = featureCode == null ? "" : featureCode.trim().toUpperCase();
        if (normalizedCode.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/bad-request",
                    "Bad Request",
                    "featureCode is required");
        }
        String payload = configJson == null || configJson.isBlank() ? "{}" : configJson;
        jdbcTemplate.update(
                "insert into workspace_feature_flag (id, workspace_id, feature_code, enabled, config_json, updated_at) " +
                        "values (?, ?, ?, ?, ?::jsonb, now()) " +
                        "on conflict (workspace_id, feature_code) do update set enabled = excluded.enabled, config_json = excluded.config_json, updated_at = now()",
                UUID.randomUUID(), workspaceId, normalizedCode, enabled, payload
        );

        return jdbcTemplate.query(
                "select id, workspace_id, feature_code, enabled, config_json::text as config_json, updated_at " +
                        "from workspace_feature_flag where workspace_id = ? and feature_code = ?",
                rs -> {
                    if (!rs.next()) {
                        throw new IllegalStateException("feature flag upsert failed");
                    }
                    return new FeatureFlagItem(
                            (UUID) rs.getObject("id"),
                            (UUID) rs.getObject("workspace_id"),
                            rs.getString("feature_code"),
                            rs.getBoolean("enabled"),
                            rs.getString("config_json"),
                            rs.getTimestamp("updated_at").toInstant()
                    );
                },
                workspaceId, normalizedCode
        );
    }

    private ApprovalRow fetchApprovalRequest(UUID requestId) {
        return jdbcTemplate.query(
                "select id, job_id, request_type, status from approval_request where id = ?",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND,
                                "https://example.com/problems/not-found",
                                "Not Found",
                                "approval request not found");
                    }
                    return new ApprovalRow(
                            (UUID) rs.getObject("id"),
                            (UUID) rs.getObject("job_id"),
                            rs.getString("request_type"),
                            rs.getString("status")
                    );
                },
                requestId
        );
    }

    private void triggerJobDispatchIfReady(UUID jobId) {
        JobRow job = jdbcTemplate.query(
                "select id, status, original_filename, file_uri from import_job where id = ?",
                rs -> rs.next() ? new JobRow(
                        (UUID) rs.getObject("id"),
                        rs.getString("status"),
                        rs.getString("original_filename"),
                        rs.getString("file_uri")
                ) : null,
                jobId
        );
        if (job == null || !"CREATED".equals(job.status())) {
            return;
        }
        importJobDispatchPort.dispatch(job.id(), extractExtension(job.originalFilename(), job.fileUri()));
    }

    private String extractExtension(String originalFilename, String fileUri) {
        String src = (originalFilename != null && !originalFilename.isBlank()) ? originalFilename : fileUri;
        if (src == null) {
            return "unknown";
        }
        int idx = src.lastIndexOf('.');
        if (idx < 0 || idx == src.length() - 1) {
            return "unknown";
        }
        return src.substring(idx + 1).toLowerCase();
    }

    private String normalizeDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/bad-request",
                    "Bad Request",
                    "decision is required");
        }
        String normalized = decision.trim().toUpperCase();
        if (!"APPROVE".equals(normalized) && !"REJECT".equals(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/bad-request",
                    "Bad Request",
                    "decision must be APPROVE or REJECT");
        }
        return normalized;
    }

    private record ApprovalRow(UUID id, UUID jobId, String requestType, String status) {
    }

    private record JobRow(UUID id, String status, String originalFilename, String fileUri) {
    }
}
