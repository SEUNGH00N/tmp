package com.example.excelimport.userwas.dto;

import java.util.List;

public record UserImportRowListResponse(
        List<UserImportRowItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
