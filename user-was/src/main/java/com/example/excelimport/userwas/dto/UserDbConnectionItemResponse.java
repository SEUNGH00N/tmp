package com.example.excelimport.userwas.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDbConnectionItemResponse(
        UUID id,
        String name,
        String dbType,
        String host,
        int port,
        String dbName,
        String username,
        String secretRef,
        String sslMode,
        Integer keyVersion,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
