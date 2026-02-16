package com.example.excelimport.dto;

import java.util.List;

public record PagedExcelRowsResponse(
        List<ExcelRowItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
