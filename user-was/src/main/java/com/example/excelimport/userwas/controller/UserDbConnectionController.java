package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionCreateRequest;
import com.example.excelimport.userwas.dto.UserDbConnectionCreateResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionListResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionTestResponse;
import com.example.excelimport.userwas.service.UserDbConnectionService;
import com.example.excelimport.userwas.web.RequestIdResolver;
import com.example.excelimport.userwas.web.WorkspaceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/db-connections")
public class UserDbConnectionController {

    private final UserDbConnectionService userDbConnectionService;
    private final WorkspaceContextResolver workspaceContextResolver;
    private final RequestIdResolver requestIdResolver;

    public UserDbConnectionController(UserDbConnectionService userDbConnectionService,
                                      WorkspaceContextResolver workspaceContextResolver,
                                      RequestIdResolver requestIdResolver) {
        this.userDbConnectionService = userDbConnectionService;
        this.workspaceContextResolver = workspaceContextResolver;
        this.requestIdResolver = requestIdResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDbConnectionCreateResponse>> create(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestBody UserDbConnectionCreateRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, servletRequest);
        UserDbConnectionCreateResponse data = userDbConnectionService.create(effectiveWorkspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, requestIdResolver.resolve(servletRequest)));
    }

    @GetMapping
    public ApiResponse<UserDbConnectionListResponse> list(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest servletRequest
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, servletRequest);
        return ApiResponse.success(userDbConnectionService.list(effectiveWorkspaceId), requestIdResolver.resolve(servletRequest));
    }

    @PostMapping("/{connectionId}/test")
    public ApiResponse<UserDbConnectionTestResponse> test(
            @PathVariable UUID connectionId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest servletRequest
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, servletRequest);
        return ApiResponse.success(userDbConnectionService.test(effectiveWorkspaceId, connectionId), requestIdResolver.resolve(servletRequest));
    }
}
