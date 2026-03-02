package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserApprovalRequestCreateRequest;
import com.example.excelimport.userwas.dto.UserApprovalRequestItem;
import com.example.excelimport.userwas.dto.UserFeatureFlagItem;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.service.UserGovernanceService;
import com.example.excelimport.userwas.web.WorkspaceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGovernanceControllerTest {

    @Mock
    private UserGovernanceService userGovernanceService;

    @Mock
    private WorkspaceContextResolver workspaceContextResolver;

    @Mock
    private HttpServletRequest request;

    @Test
    void requestImportApprovalUsesResolvedWorkspaceId() {
        UserGovernanceController controller = new UserGovernanceController(userGovernanceService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UserApprovalRequestCreateRequest body = new UserApprovalRequestCreateRequest("PLATFORM_ADMIN", "need review");
        UUID createdId = UUID.randomUUID();

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userGovernanceService.createImportApprovalRequest(
                workspaceId, jobId, "workspace-user", "PLATFORM_ADMIN", "need review"
        )).thenReturn(createdId);

        ApiResponse<UUID> response = controller.requestImportApproval(jobId, workspaceId, tenantId, body, request);

        assertEquals(true, response.success());
        assertEquals(createdId, response.data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userGovernanceService).createImportApprovalRequest(
                workspaceId, jobId, "workspace-user", "PLATFORM_ADMIN", "need review"
        );
    }

    @Test
    void listApprovalRequestsUsesResolvedWorkspaceId() {
        UserGovernanceController controller = new UserGovernanceController(userGovernanceService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        List<UserApprovalRequestItem> items = List.of(
                new UserApprovalRequestItem(UUID.randomUUID(), workspaceId, UUID.randomUUID(),
                        "IMPORT_EXECUTION", "PLATFORM_ADMIN", "PENDING", "workspace-user", Instant.now())
        );

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userGovernanceService.listApprovalRequests(workspaceId)).thenReturn(items);

        ApiResponse<List<UserApprovalRequestItem>> response = controller.listApprovalRequests(workspaceId, tenantId, request);

        assertEquals(true, response.success());
        assertEquals(items, response.data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userGovernanceService).listApprovalRequests(workspaceId);
    }

    @Test
    void listFeatureFlagsUsesResolvedWorkspaceId() {
        UserGovernanceController controller = new UserGovernanceController(userGovernanceService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        List<UserFeatureFlagItem> items = List.of(
                new UserFeatureFlagItem("APPROVAL_REQUIRED", true, "{}", Instant.now())
        );

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userGovernanceService.listFeatureFlags(workspaceId)).thenReturn(items);

        ApiResponse<List<UserFeatureFlagItem>> response = controller.listFeatureFlags(workspaceId, tenantId, request);

        assertEquals(true, response.success());
        assertEquals(items, response.data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userGovernanceService).listFeatureFlags(workspaceId);
    }

    @Test
    void listApprovalRequestsPropagatesWorkspaceRequiredException() {
        UserGovernanceController controller = new UserGovernanceController(userGovernanceService, workspaceContextResolver);
        UUID tenantId = UUID.randomUUID();

        when(workspaceContextResolver.resolve(null, tenantId, request))
                .thenThrow(new UserWasException(400, "workspace_id is required"));

        UserWasException exception = assertThrows(UserWasException.class,
                () -> controller.listApprovalRequests(null, tenantId, request));

        assertEquals(400, exception.getStatus());
        assertEquals("workspace_id is required", exception.getMessage());
    }
}

