package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserBillingSummaryResponse;
import com.example.excelimport.userwas.dto.UserPlanResponse;
import com.example.excelimport.userwas.dto.UserSubscriptionResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.service.UserBillingService;
import com.example.excelimport.userwas.web.WorkspaceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBillingControllerTest {

    @Mock
    private UserBillingService userBillingService;

    @Mock
    private WorkspaceContextResolver workspaceContextResolver;

    @Mock
    private HttpServletRequest request;

    @Test
    void planReturnsSuccessWithResolvedWorkspace() {
        UserBillingController controller = new UserBillingController(userBillingService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserPlanResponse planResponse = new UserPlanResponse("FREE", "Free", 100, 20);

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userBillingService.getCurrentPlan(workspaceId)).thenReturn(planResponse);

        ApiResponse<UserPlanResponse> response = controller.plan(workspaceId, tenantId, request);

        assertEquals(true, response.success());
        assertEquals(planResponse, response.data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userBillingService).getCurrentPlan(workspaceId);
    }

    @Test
    void subscriptionReturnsSuccessWithResolvedWorkspace() {
        UserBillingController controller = new UserBillingController(userBillingService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserSubscriptionResponse subscriptionResponse = new UserSubscriptionResponse(
                "ACTIVE", true, "MANUAL", Instant.now(), Instant.now(), null, null
        );

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userBillingService.getCurrentSubscription(workspaceId)).thenReturn(subscriptionResponse);

        ApiResponse<UserSubscriptionResponse> response = controller.subscription(workspaceId, tenantId, request);

        assertEquals(true, response.success());
        assertEquals(subscriptionResponse, response.data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userBillingService).getCurrentSubscription(workspaceId);
    }

    @Test
    void summaryReturnsSuccessWithResolvedWorkspace() {
        UserBillingController controller = new UserBillingController(userBillingService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserBillingSummaryResponse summaryResponse = new UserBillingSummaryResponse(
                new UserPlanResponse("PRO", "Pro", 500, 100),
                new UserSubscriptionResponse("ACTIVE", true, "MANUAL", Instant.now(), Instant.now(), null, null),
                10, 490, 1, 200, 0, 1000
        );

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userBillingService.getSummary(workspaceId)).thenReturn(summaryResponse);

        ApiResponse<UserBillingSummaryResponse> response = controller.summary(workspaceId, tenantId, request);

        assertEquals(true, response.success());
        assertEquals(summaryResponse, response.data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userBillingService).getSummary(workspaceId);
    }

    @Test
    void planPropagatesWorkspaceRequiredException() {
        UserBillingController controller = new UserBillingController(userBillingService, workspaceContextResolver);
        UUID tenantId = UUID.randomUUID();

        when(workspaceContextResolver.resolve(null, tenantId, request))
                .thenThrow(new UserWasException(400, "workspace_id is required"));

        UserWasException exception = assertThrows(UserWasException.class, () -> controller.plan(null, tenantId, request));

        assertEquals(400, exception.getStatus());
        assertEquals("workspace_id is required", exception.getMessage());
    }
}

