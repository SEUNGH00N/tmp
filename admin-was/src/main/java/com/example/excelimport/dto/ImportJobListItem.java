package com.example.excelimport.dto;

import com.example.excelimport.entity.ImportJobStatus;

import java.time.Instant;
import java.util.UUID;

public record ImportJobListItem(
        UUID jobId,
        UUID workspaceId,
        ImportJobStatus status,
        int progressPct,
        int totalRows,
        int processedRows,
        int successCount,
        int failCount,
        Instant createdAt
) {
}
