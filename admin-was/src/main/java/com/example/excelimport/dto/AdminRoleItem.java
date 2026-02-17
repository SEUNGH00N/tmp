package com.example.excelimport.dto;

import java.util.UUID;

public record AdminRoleItem(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        long userCount
) {
}
