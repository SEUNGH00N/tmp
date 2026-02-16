package com.example.excelimport.userwas.dto;

import java.util.List;

public record UserImportErrorListResponse(
        List<UserImportErrorItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
