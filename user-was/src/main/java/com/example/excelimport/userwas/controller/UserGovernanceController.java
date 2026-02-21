package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserApprovalRequestCreateRequest;
import com.example.excelimport.userwas.dto.UserApprovalRequestItem;
import com.example.excelimport.userwas.dto.UserFeatureFlagItem;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.service.UserGovernanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

    private static final String WS_SESSION_KEY = "USER_WORKSPACE_ID";
    private final UserGovernanceService userGovernanceService;

    public UserGovernanceController(UserGovernanceService userGovernanceService) {
        this.userGovernanceService = userGovernanceService;
    }

    @PostMapping("/approvals/imports/{jobId}/request")
    public ApiResponse<UUID> requestImportApproval(
            @PathVariable UUID jobId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestBody(required = false) UserApprovalRequestCreateRequest body,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        String requestedBy = "workspace-user";
        String mode = body == null ? null : body.policyMode();
        String reason = body == null ? null : body.reason();
        UUID id = userGovernanceService.createImportApprovalRequest(effectiveWorkspaceId, jobId, requestedBy, mode, reason);
        return ApiResponse.success(id);
    }

    @GetMapping("/approvals/requests")
    public ApiResponse<List<UserApprovalRequestItem>> listApprovalRequests(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userGovernanceService.listApprovalRequests(effectiveWorkspaceId));
    }

    @GetMapping("/feature-flags")
    public ApiResponse<List<UserFeatureFlagItem>> listFeatureFlags(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userGovernanceService.listFeatureFlags(effectiveWorkspaceId));
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
