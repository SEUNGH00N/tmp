package com.example.excelimport.dto;

import com.example.excelimport.entity.ImportJobStatus;

import java.util.UUID;

public record ImportStatusResponse(
        UUID jobId,
        ImportJobStatus status,
        int progressPct,
        int totalRows,
        int processedRows
) {
}
