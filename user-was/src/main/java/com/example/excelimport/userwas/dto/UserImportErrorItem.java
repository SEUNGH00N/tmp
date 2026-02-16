package com.example.excelimport.userwas.dto;

import java.time.Instant;

public record UserImportErrorItem(
        int rowIndex,
        String columnName,
        String errorCode,
        String errorMsg,
        Instant createdAt
) {
}
