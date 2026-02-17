package com.example.excelimport.common.web;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, Meta meta) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, new Meta(null, Instant.now().toString()));
    }

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(true, data, new Meta(requestId, Instant.now().toString()));
    }

    public record Meta(String requestId, String timestamp) {
    }
}
