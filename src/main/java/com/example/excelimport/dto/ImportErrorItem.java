package com.example.excelimport.dto;

public record ImportErrorItem(
        int rowIndex,
        String columnName,
        String errorCode,
        String errorMsg
) {
}
