package com.example.excelimport.dto;

import java.time.Instant;
import java.util.UUID;

public record FeatureFlagItem(
        UUID id,
        UUID workspaceId,
        String featureCode,
        boolean enabled,
        String configJson,
        Instant updatedAt
) {
}
