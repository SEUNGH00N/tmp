package com.example.excelimport.userwas.dto;

import java.time.Instant;

public record UserSubscriptionResponse(
        String status,
        boolean current,
        String provider,
        Instant startedAt,
        Instant billingCycleAnchor,
        Instant renewedAt,
        Instant canceledAt
) {
}
