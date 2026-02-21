package com.example.excelimport.userwas.service;

import java.time.Instant;
import java.util.UUID;

public record UserImportProcessRequestedEvent(
        UUID jobId,
        UUID runId,
        int runNo,
        String extension,
        String fileUri,
        Instant requestedAt
) {
}
