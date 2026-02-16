package com.example.excelimport.userwas.dto;

import java.util.UUID;

public record UserImportStatusResponse(
        UUID jobId,
        String status,
        int progressPct,
        int totalRows,
        int processedRows
) {
}
