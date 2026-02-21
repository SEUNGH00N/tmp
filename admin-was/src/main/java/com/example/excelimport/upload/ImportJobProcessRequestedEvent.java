package com.example.excelimport.upload;

import java.time.Instant;
import java.util.UUID;

public record ImportJobProcessRequestedEvent(
        UUID jobId,
        String extension,
        Instant requestedAt
) {
}
