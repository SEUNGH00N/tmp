package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserImportCreateResponse;
import com.example.excelimport.userwas.dto.UserImportErrorListResponse;
import com.example.excelimport.userwas.dto.UserImportListResponse;
import com.example.excelimport.userwas.dto.UserImportRowListResponse;
import com.example.excelimport.userwas.dto.UserImportStatusResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.service.UserImportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

    private static final String WS_SESSION_KEY = "USER_WORKSPACE_ID";

    private final UserImportService userImportService;

    public UserImportController(UserImportService userImportService) {
        this.userImportService = userImportService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UserImportCreateResponse>> create(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        UserImportCreateResponse data = userImportService.create(effectiveWorkspaceId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    @GetMapping
    public ApiResponse<UserImportListResponse> list(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userImportService.list(effectiveWorkspaceId, page, size));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<UserImportStatusResponse> status(
            @PathVariable UUID jobId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userImportService.status(effectiveWorkspaceId, jobId));
    }

    @GetMapping("/{jobId}/rows")
    public ApiResponse<UserImportRowListResponse> rows(
            @PathVariable UUID jobId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userImportService.rows(effectiveWorkspaceId, jobId, page, size));
    }

    @GetMapping("/{jobId}/errors")
    public ApiResponse<UserImportErrorListResponse> errors(
            @PathVariable UUID jobId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userImportService.errors(effectiveWorkspaceId, jobId, page, size));
    }

    @PostMapping("/session/workspace")
    public ApiResponse<Void> setWorkspaceSession(@RequestParam("workspace_id") UUID workspaceId, HttpServletRequest request) {
        request.getSession(true).setAttribute(WS_SESSION_KEY, workspaceId.toString());
        return ApiResponse.success(null);
    }

    @GetMapping("/session/workspace")
    public ApiResponse<String> getWorkspaceSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String workspaceId = session == null ? null : (String) session.getAttribute(WS_SESSION_KEY);
        return ApiResponse.success(workspaceId);
    }

    private UUID resolveWorkspaceId(UUID workspaceId, UUID tenantId, HttpServletRequest request) {
        if (workspaceId != null) {
            request.getSession(true).setAttribute(WS_SESSION_KEY, workspaceId.toString());
            return workspaceId;
        }
        if (tenantId != null) {
            request.getSession(true).setAttribute(WS_SESSION_KEY, tenantId.toString());
            return tenantId;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute(WS_SESSION_KEY);
            if (attr != null) {
                return UUID.fromString(attr.toString());
            }
        }
        throw new UserWasException(400, "workspace_id is required");
    }
}
