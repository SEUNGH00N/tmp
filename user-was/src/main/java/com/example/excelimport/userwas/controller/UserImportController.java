package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserImportCreateResponse;
import com.example.excelimport.userwas.dto.UserImportErrorListResponse;
import com.example.excelimport.userwas.dto.UserImportListResponse;
import com.example.excelimport.userwas.dto.UserImportRowListResponse;
import com.example.excelimport.userwas.dto.UserImportStatusResponse;
import com.example.excelimport.userwas.service.UserImportService;
import com.example.excelimport.userwas.web.RequestIdResolver;
import com.example.excelimport.userwas.web.WorkspaceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
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
    private final WorkspaceContextResolver workspaceContextResolver;
    private final RequestIdResolver requestIdResolver;

    public UserImportController(UserImportService userImportService,
                                WorkspaceContextResolver workspaceContextResolver,
                                RequestIdResolver requestIdResolver) {
        this.userImportService = userImportService;
        this.workspaceContextResolver = workspaceContextResolver;
        this.requestIdResolver = requestIdResolver;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UserImportCreateResponse>> create(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        UserImportCreateResponse data = userImportService.create(effectiveWorkspaceId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, requestIdResolver.resolve(request)));
    }

    @GetMapping
    public ApiResponse<UserImportListResponse> list(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userImportService.list(effectiveWorkspaceId, page, size), requestIdResolver.resolve(request));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<UserImportStatusResponse> status(
            @PathVariable UUID jobId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userImportService.status(effectiveWorkspaceId, jobId), requestIdResolver.resolve(request));
    }

    @GetMapping("/{jobId}/rows")
    public ApiResponse<UserImportRowListResponse> rows(
            @PathVariable UUID jobId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userImportService.rows(effectiveWorkspaceId, jobId, page, size), requestIdResolver.resolve(request));
    }

    @GetMapping("/{jobId}/errors")
    public ApiResponse<UserImportErrorListResponse> errors(
            @PathVariable UUID jobId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userImportService.errors(effectiveWorkspaceId, jobId, page, size), requestIdResolver.resolve(request));
    }

    @PostMapping("/session/workspace")
    public ApiResponse<Void> setWorkspaceSession(@RequestParam("workspace_id") UUID workspaceId, HttpServletRequest request) {
        workspaceContextResolver.setWorkspaceSession(request, workspaceId);
        return ApiResponse.success(null, requestIdResolver.resolve(request));
    }

    @GetMapping("/session/workspace")
    public ApiResponse<String> getWorkspaceSession(HttpServletRequest request) {
        return ApiResponse.success(workspaceContextResolver.getWorkspaceSession(request), requestIdResolver.resolve(request));
    }
}
