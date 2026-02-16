package com.example.excelimport.userwas.dto;

import java.time.Instant;
import java.util.UUID;

public record UserImportRowItem(
        UUID id,
        String payloadJson,
        Instant createdAt
) {
}
