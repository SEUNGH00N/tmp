package com.example.excelimport.userwas.dto;

public record UserApprovalRequestCreateRequest(
        String policyMode,
        String reason
) {
}
