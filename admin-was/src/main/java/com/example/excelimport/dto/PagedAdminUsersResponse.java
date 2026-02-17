package com.example.excelimport.dto;

import java.util.List;

public record PagedAdminUsersResponse(
        List<AdminUserItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
