package com.example.excelimport.userwas.dto;

import java.time.Instant;

public record UserFeatureFlagItem(
        String featureCode,
        boolean enabled,
        String configJson,
        Instant updatedAt
) {
}
