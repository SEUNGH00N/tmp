package com.example.excelimport.dto;

import java.util.List;

public record PagedAppUsersResponse(
        List<AppUserItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
