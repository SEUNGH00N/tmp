package com.example.excelimport.userwas.dto;

public record UserBillingSummaryResponse(
        UserPlanResponse plan,
        UserSubscriptionResponse subscription,
        long monthlyUploadedFiles,
        long remainingMonthlyUploads,
        long todayUploadedFiles,
        long todayProcessedRows,
        long todayFailedRows,
        long todayStorageBytes
) {
}
