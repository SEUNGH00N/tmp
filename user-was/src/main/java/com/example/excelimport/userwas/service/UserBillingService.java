package com.example.excelimport.userwas.service;

import com.example.excelimport.userwas.dto.UserBillingSummaryResponse;
import com.example.excelimport.userwas.dto.UserPlanResponse;
import com.example.excelimport.userwas.dto.UserSubscriptionResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class UserBillingService {

    private final JdbcTemplate jdbcTemplate;

    public UserBillingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserPlanResponse getCurrentPlan(UUID workspaceId) {
        PlanInfo info = fetchCurrentPlanInfo(workspaceId);
        return new UserPlanResponse(info.code(), info.name(), info.monthlyUploadQuota(), info.maxFileSizeMb());
    }

    public UserSubscriptionResponse getCurrentSubscription(UUID workspaceId) {
        return jdbcTemplate.query(
                "select s.status, s.current_flag, s.provider, s.started_at, s.billing_cycle_anchor, s.renewed_at, s.canceled_at " +
                        "from workspace_subscription s " +
                        "where s.workspace_id = ? and s.current_flag = true " +
                        "order by s.started_at desc limit 1",
                rs -> {
                    if (!rs.next()) {
                        throw new UserWasException(400, "active subscription not found");
                    }
                    return new UserSubscriptionResponse(
                            rs.getString("status"),
                            rs.getBoolean("current_flag"),
                            rs.getString("provider"),
                            toInstant(rs.getTimestamp("started_at")),
                            toInstant(rs.getTimestamp("billing_cycle_anchor")),
                            toInstant(rs.getTimestamp("renewed_at")),
                            toInstant(rs.getTimestamp("canceled_at"))
                    );
                },
                workspaceId
        );
    }

    public UserBillingSummaryResponse getSummary(UUID workspaceId) {
        UserPlanResponse plan = getCurrentPlan(workspaceId);
        UserSubscriptionResponse subscription = getCurrentSubscription(workspaceId);

        Long monthlyUploaded = jdbcTemplate.queryForObject(
                "select coalesce(sum(uploaded_files), 0) " +
                        "from workspace_usage_daily " +
                        "where workspace_id = ? " +
                        "and usage_day >= date_trunc('month', current_date)::date " +
                        "and usage_day < (date_trunc('month', current_date) + interval '1 month')::date",
                Long.class,
                workspaceId
        );
        long monthlyUploadedFiles = monthlyUploaded == null ? 0 : monthlyUploaded;
        long remaining = Math.max(0, plan.monthlyUploadQuota() - monthlyUploadedFiles);

        LocalDate today = LocalDate.now();
        UsageInfo todayUsage = jdbcTemplate.query(
                "select uploaded_files, processed_rows, failed_rows, storage_bytes " +
                        "from workspace_usage_daily where workspace_id = ? and usage_day = ?",
                rs -> {
                    if (!rs.next()) {
                        return new UsageInfo(0, 0, 0, 0);
                    }
                    return new UsageInfo(
                            rs.getLong("uploaded_files"),
                            rs.getLong("processed_rows"),
                            rs.getLong("failed_rows"),
                            rs.getLong("storage_bytes")
                    );
                },
                workspaceId, today
        );

        return new UserBillingSummaryResponse(
                plan,
                subscription,
                monthlyUploadedFiles,
                remaining,
                todayUsage.uploadedFiles(),
                todayUsage.processedRows(),
                todayUsage.failedRows(),
                todayUsage.storageBytes()
        );
    }

    public void validateUploadQuota(UUID workspaceId, long fileSizeBytes) {
        PlanInfo plan = fetchCurrentPlanInfo(workspaceId);

        long maxFileSizeBytes = plan.maxFileSizeMb() * 1024L * 1024L;
        if (fileSizeBytes > maxFileSizeBytes) {
            throw new UserWasException(413, "file exceeds plan max size: " + plan.maxFileSizeMb() + "MB");
        }

        Long monthlyUploaded = jdbcTemplate.queryForObject(
                "select coalesce(sum(uploaded_files), 0) " +
                        "from workspace_usage_daily " +
                        "where workspace_id = ? " +
                        "and usage_day >= date_trunc('month', current_date)::date " +
                        "and usage_day < (date_trunc('month', current_date) + interval '1 month')::date",
                Long.class,
                workspaceId
        );
        long used = monthlyUploaded == null ? 0 : monthlyUploaded;
        if (used >= plan.monthlyUploadQuota()) {
            throw new UserWasException(429, "monthly upload quota exceeded");
        }
    }

    public void addUploadAcceptedUsage(UUID workspaceId, long fileSizeBytes) {
        upsertUsage(workspaceId, 1, 0, 0, fileSizeBytes, 1);
    }

    public void addProcessingOutcome(UUID workspaceId, long processedRows, long failedRows) {
        upsertUsage(workspaceId, 0, processedRows, failedRows, 0, 0);
    }

    private void upsertUsage(UUID workspaceId,
                             int uploadedFilesDelta,
                             long processedRowsDelta,
                             long failedRowsDelta,
                             long storageBytesDelta,
                             int jobCountDelta) {
        LocalDate today = LocalDate.now();
        jdbcTemplate.update(
                "insert into workspace_usage_daily " +
                        "(id, workspace_id, usage_day, uploaded_files, processed_rows, failed_rows, storage_bytes, job_count, created_at, updated_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, now(), now()) " +
                        "on conflict (workspace_id, usage_day) do update set " +
                        "uploaded_files = workspace_usage_daily.uploaded_files + excluded.uploaded_files, " +
                        "processed_rows = workspace_usage_daily.processed_rows + excluded.processed_rows, " +
                        "failed_rows = workspace_usage_daily.failed_rows + excluded.failed_rows, " +
                        "storage_bytes = workspace_usage_daily.storage_bytes + excluded.storage_bytes, " +
                        "job_count = workspace_usage_daily.job_count + excluded.job_count, " +
                        "updated_at = now()",
                UUID.randomUUID(), workspaceId, today,
                uploadedFilesDelta, processedRowsDelta, failedRowsDelta, storageBytesDelta, jobCountDelta
        );
    }

    private PlanInfo fetchCurrentPlanInfo(UUID workspaceId) {
        return jdbcTemplate.query(
                "select p.code, p.name, p.monthly_upload_quota, p.max_file_size_mb " +
                        "from workspace_subscription s " +
                        "join billing_plan p on s.billing_plan_id = p.id " +
                        "where s.workspace_id = ? and s.current_flag = true and s.status = 'ACTIVE' " +
                        "order by s.started_at desc limit 1",
                rs -> {
                    if (!rs.next()) {
                        throw new UserWasException(400, "active subscription not found");
                    }
                    return new PlanInfo(
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getInt("monthly_upload_quota"),
                            rs.getLong("max_file_size_mb")
                    );
                },
                workspaceId
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record PlanInfo(String code, String name, int monthlyUploadQuota, long maxFileSizeMb) {
    }

    private record UsageInfo(long uploadedFiles, long processedRows, long failedRows, long storageBytes) {
    }
}
