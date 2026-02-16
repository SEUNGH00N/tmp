package com.example.excelimport.userwas.dto;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, Meta meta) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, new Meta(Instant.now().toString()));
    }

    public record Meta(String timestamp) {}
}
