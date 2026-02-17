package com.example.excelimport.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserItem(
        UUID id,
        String username,
        String displayName,
        boolean active,
        Instant createdAt,
        List<String> roles
) {
}
