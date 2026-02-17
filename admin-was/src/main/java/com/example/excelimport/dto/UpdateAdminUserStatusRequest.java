package com.example.excelimport.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAdminUserStatusRequest(
        @NotNull Boolean active
) {
}
