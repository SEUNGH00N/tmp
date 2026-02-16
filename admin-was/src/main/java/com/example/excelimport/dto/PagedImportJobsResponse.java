package com.example.excelimport.dto;

import java.util.List;

public record PagedImportJobsResponse(
        List<ImportJobListItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
