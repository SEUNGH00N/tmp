package com.example.excelimport.dto;

public record FeatureFlagUpsertRequest(
        boolean enabled,
        String configJson
) {
}
