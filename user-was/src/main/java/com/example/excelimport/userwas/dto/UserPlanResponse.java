package com.example.excelimport.userwas.dto;

public record UserPlanResponse(
        String code,
        String name,
        int monthlyUploadQuota,
        long maxFileSizeMb
) {
}
