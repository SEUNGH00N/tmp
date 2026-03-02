package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserApprovalRequestCreateRequest;
import com.example.excelimport.userwas.dto.UserApprovalRequestItem;
import com.example.excelimport.userwas.dto.UserFeatureFlagItem;
import com.example.excelimport.userwas.service.UserGovernanceService;
import com.example.excelimport.userwas.web.RequestIdResolver;
import com.example.excelimport.userwas.web.WorkspaceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
public class UserGovernanceController {

    private final UserGovernanceService userGovernanceService;
    private final WorkspaceContextResolver workspaceContextResolver;
    private final RequestIdResolver requestIdResolver;

    public UserGovernanceController(UserGovernanceService userGovernanceService,
                                    WorkspaceContextResolver workspaceContextResolver,
                                    RequestIdResolver requestIdResolver) {
        this.userGovernanceService = userGovernanceService;
        this.workspaceContextResolver = workspaceContextResolver;
        this.requestIdResolver = requestIdResolver;
    }

    @PostMapping("/approvals/imports/{jobId}/request")
    public ApiResponse<UUID> requestImportApproval(
            @PathVariable UUID jobId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestBody(required = false) UserApprovalRequestCreateRequest body,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        String requestedBy = "workspace-user";
        String mode = body == null ? null : body.policyMode();
        String reason = body == null ? null : body.reason();
        UUID id = userGovernanceService.createImportApprovalRequest(effectiveWorkspaceId, jobId, requestedBy, mode, reason);
        return ApiResponse.success(id, requestIdResolver.resolve(request));
    }

    @GetMapping("/approvals/requests")
    public ApiResponse<List<UserApprovalRequestItem>> listApprovalRequests(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userGovernanceService.listApprovalRequests(effectiveWorkspaceId), requestIdResolver.resolve(request));
    }

    @GetMapping("/feature-flags")
    public ApiResponse<List<UserFeatureFlagItem>> listFeatureFlags(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userGovernanceService.listFeatureFlags(effectiveWorkspaceId), requestIdResolver.resolve(request));
    }
}
