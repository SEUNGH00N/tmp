package com.example.excelimport.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateAdminUserRolesRequest(
        @NotEmpty List<String> roleCodes
) {
}
