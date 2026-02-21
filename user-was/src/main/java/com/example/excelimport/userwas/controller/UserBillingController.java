package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserBillingSummaryResponse;
import com.example.excelimport.userwas.dto.UserPlanResponse;
import com.example.excelimport.userwas.dto.UserSubscriptionResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.service.UserBillingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/billing")
public class UserBillingController {

    private static final String WS_SESSION_KEY = "USER_WORKSPACE_ID";

    private final UserBillingService userBillingService;

    public UserBillingController(UserBillingService userBillingService) {
        this.userBillingService = userBillingService;
    }

    @GetMapping("/plan")
    public ApiResponse<UserPlanResponse> plan(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userBillingService.getCurrentPlan(effectiveWorkspaceId));
    }

    @GetMapping("/subscription")
    public ApiResponse<UserSubscriptionResponse> subscription(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userBillingService.getCurrentSubscription(effectiveWorkspaceId));
    }

    @GetMapping("/summary")
    public ApiResponse<UserBillingSummaryResponse> summary(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, request);
        return ApiResponse.success(userBillingService.getSummary(effectiveWorkspaceId));
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
