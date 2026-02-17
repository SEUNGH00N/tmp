package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserImportCreateResponse;
import com.example.excelimport.userwas.dto.UserImportErrorListResponse;
import com.example.excelimport.userwas.dto.UserImportListResponse;
import com.example.excelimport.userwas.dto.UserImportRowListResponse;
import com.example.excelimport.userwas.dto.UserImportStatusResponse;
import com.example.excelimport.userwas.service.UserImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/imports")
public class UserImportController {

    private final UserImportService userImportService;

    public UserImportController(UserImportService userImportService) {
        this.userImportService = userImportService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UserImportCreateResponse>> create(
            @RequestParam("tenant_id") UUID tenantId,
            @RequestParam("file") MultipartFile file) {
        UserImportCreateResponse data = userImportService.create(tenantId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    @GetMapping
    public ApiResponse<UserImportListResponse> list(
            @RequestParam("tenant_id") UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(userImportService.list(tenantId, page, size));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<UserImportStatusResponse> status(
            @PathVariable UUID jobId,
            @RequestParam("tenant_id") UUID tenantId) {
        return ApiResponse.success(userImportService.status(tenantId, jobId));
    }

    @GetMapping("/{jobId}/rows")
    public ApiResponse<UserImportRowListResponse> rows(
            @PathVariable UUID jobId,
            @RequestParam("tenant_id") UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ApiResponse.success(userImportService.rows(tenantId, jobId, page, size));
    }

    @GetMapping("/{jobId}/errors")
    public ApiResponse<UserImportErrorListResponse> errors(
            @PathVariable UUID jobId,
            @RequestParam("tenant_id") UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ApiResponse.success(userImportService.errors(tenantId, jobId, page, size));
    }
}
