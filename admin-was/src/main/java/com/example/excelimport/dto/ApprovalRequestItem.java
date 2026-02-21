package com.example.excelimport.dto;

import java.time.Instant;
import java.util.UUID;

public record ApprovalRequestItem(
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
