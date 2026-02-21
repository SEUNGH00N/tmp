package com.example.excelimport.userwas.service;

import com.example.excelimport.userwas.dto.UserApprovalRequestItem;
import com.example.excelimport.userwas.dto.UserFeatureFlagItem;
import com.example.excelimport.userwas.exception.UserWasException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserGovernanceService {

    private final JdbcTemplate jdbcTemplate;

    public UserGovernanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID createImportApprovalRequest(UUID workspaceId,
                                            UUID jobId,
                                            String requestedBy,
                                            String policyMode,
                                            String reason) {
        ensureJobOwned(workspaceId, jobId);
        String mode = normalizePolicyMode(policyMode);

        UUID existingPending = jdbcTemplate.query(
                "select id from approval_request where workspace_id = ? and job_id = ? and request_type = 'IMPORT_EXECUTION' and status = 'PENDING' order by created_at desc limit 1",
                rs -> rs.next() ? (UUID) rs.getObject("id") : null,
                workspaceId, jobId
        );
        if (existingPending != null) {
            return existingPending;
        }

        UUID requestId = UUID.randomUUID();
        String status = "OWNER".equals(mode) ? "APPROVED" : "PENDING";
        jdbcTemplate.update(
                "insert into approval_request (id, workspace_id, job_id, request_type, policy_mode, status, requested_by, created_at, updated_at) " +
                        "values (?, ?, ?, 'IMPORT_EXECUTION', ?, ?, ?, now(), now())",
                requestId, workspaceId, jobId, mode, status, requestedBy
        );

        if ("OWNER".equals(mode)) {
            jdbcTemplate.update(
                    "insert into approval_decision (id, approval_request_id, decided_by, decision, reason, decided_at, created_at) " +
                            "values (?, ?, ?, 'APPROVE', ?, now(), now())",
                    UUID.randomUUID(), requestId, requestedBy, reason
            );
        }
        return requestId;
    }

    @Transactional(readOnly = true)
    public List<UserApprovalRequestItem> listApprovalRequests(UUID workspaceId) {
        return jdbcTemplate.query(
                "select id, workspace_id, job_id, request_type, policy_mode, status, requested_by, created_at " +
                        "from approval_request where workspace_id = ? order by created_at desc limit 100",
                (rs, n) -> new UserApprovalRequestItem(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("workspace_id"),
                        (UUID) rs.getObject("job_id"),
                        rs.getString("request_type"),
                        rs.getString("policy_mode"),
                        rs.getString("status"),
                        rs.getString("requested_by"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                workspaceId
        );
    }

    @Transactional(readOnly = true)
    public List<UserFeatureFlagItem> listFeatureFlags(UUID workspaceId) {
        return jdbcTemplate.query(
                "select feature_code, enabled, config_json::text as config_json, updated_at " +
                        "from workspace_feature_flag where workspace_id = ? order by feature_code",
                (rs, n) -> new UserFeatureFlagItem(
                        rs.getString("feature_code"),
                        rs.getBoolean("enabled"),
                        rs.getString("config_json"),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                workspaceId
        );
    }

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(UUID workspaceId, String featureCode) {
        Boolean enabled = jdbcTemplate.query(
                "select enabled from workspace_feature_flag where workspace_id = ? and feature_code = ?",
                rs -> rs.next() ? rs.getBoolean("enabled") : null,
                workspaceId, featureCode
        );
        return Boolean.TRUE.equals(enabled);
    }

    private void ensureJobOwned(UUID workspaceId, UUID jobId) {
        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from import_job where id = ? and workspace_id = ?",
                Integer.class,
                jobId, workspaceId
        );
        if (exists == null || exists == 0) {
            throw new UserWasException(404, "job not found");
        }
    }

    private String normalizePolicyMode(String value) {
        String mode = (value == null || value.isBlank()) ? "PLATFORM_ADMIN" : value.trim().toUpperCase();
        if (!"OWNER".equals(mode) && !"PLATFORM_ADMIN".equals(mode) && !"DUAL_APPROVAL".equals(mode)) {
            throw new UserWasException(400, "policyMode must be OWNER|PLATFORM_ADMIN|DUAL_APPROVAL");
        }
        return mode;
    }
}
