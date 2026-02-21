package com.example.excelimport.userwas.service;

import java.time.Instant;
import java.util.UUID;

public record UserImportProcessRequestedEvent(
        UUID jobId,
        String extension,
        String fileUri,
        Instant requestedAt
) {
}
