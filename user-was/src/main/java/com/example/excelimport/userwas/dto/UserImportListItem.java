package com.example.excelimport.userwas.dto;

import java.time.Instant;
import java.util.UUID;

public record UserImportListItem(
        UUID jobId,
        UUID tenantId,
        String status,
        int progressPct,
        int totalRows,
        int processedRows,
        Instant createdAt
) {
}
