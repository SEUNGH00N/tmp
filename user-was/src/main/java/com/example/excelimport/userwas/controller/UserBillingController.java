package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserBillingSummaryResponse;
import com.example.excelimport.userwas.dto.UserPlanResponse;
import com.example.excelimport.userwas.dto.UserSubscriptionResponse;
import com.example.excelimport.userwas.service.UserBillingService;
import com.example.excelimport.userwas.web.WorkspaceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/billing")
public class UserBillingController {

    private final UserBillingService userBillingService;
    private final WorkspaceContextResolver workspaceContextResolver;

    public UserBillingController(UserBillingService userBillingService,
                                 WorkspaceContextResolver workspaceContextResolver) {
        this.userBillingService = userBillingService;
        this.workspaceContextResolver = workspaceContextResolver;
    }

    @GetMapping("/plan")
    public ApiResponse<UserPlanResponse> plan(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userBillingService.getCurrentPlan(effectiveWorkspaceId));
    }

    @GetMapping("/subscription")
    public ApiResponse<UserSubscriptionResponse> subscription(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userBillingService.getCurrentSubscription(effectiveWorkspaceId));
    }

    @GetMapping("/summary")
    public ApiResponse<UserBillingSummaryResponse> summary(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID effectiveWorkspaceId = workspaceContextResolver.resolve(workspaceId, tenantId, request);
        return ApiResponse.success(userBillingService.getSummary(effectiveWorkspaceId));
    }
}
