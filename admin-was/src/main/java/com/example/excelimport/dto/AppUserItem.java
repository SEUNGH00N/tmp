package com.example.excelimport.dto;

import java.time.Instant;
import java.util.UUID;

public record AppUserItem(
        UUID id,
        String username,
        String displayName,
        boolean active,
        Instant createdAt
) {
}
