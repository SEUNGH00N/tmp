package com.example.excelimport.dto;

import java.time.Instant;
import java.util.UUID;

public record ExcelRowItem(
        UUID id,
        String payloadJson,
        Instant createdAt
) {
}
