package com.example.excelimport.userwas.dto;

import java.util.List;

public record UserImportListResponse(
        List<UserImportListItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
