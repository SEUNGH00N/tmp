package com.example.excelimport.userwas.dto;

import java.time.Instant;
import java.util.UUID;

public record UserApprovalRequestItem(
        UUID id,
        UUID workspaceId,
        UUID jobId,
        String requestType,
        String policyMode,
        String status,
        String requestedBy,
        Instant createdAt
) {
}
