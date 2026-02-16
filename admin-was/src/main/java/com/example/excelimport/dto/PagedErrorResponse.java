package com.example.excelimport.dto;

import java.util.List;

public record PagedErrorResponse(
        List<ImportErrorItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
