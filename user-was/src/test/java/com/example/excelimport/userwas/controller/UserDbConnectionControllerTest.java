package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionCreateRequest;
import com.example.excelimport.userwas.dto.UserDbConnectionCreateResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionListResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionTestResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.service.UserDbConnectionService;
import com.example.excelimport.userwas.web.WorkspaceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDbConnectionControllerTest {

    @Mock
    private UserDbConnectionService userDbConnectionService;

    @Mock
    private WorkspaceContextResolver workspaceContextResolver;

    @Mock
    private HttpServletRequest request;

    @Test
    void createUsesResolvedWorkspaceId() {
        UserDbConnectionController controller = new UserDbConnectionController(userDbConnectionService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserDbConnectionCreateRequest body = new UserDbConnectionCreateRequest(
                "main", "POSTGRESQL", "localhost", 5432, "db", "user", "pw", null, "disable"
        );
        UserDbConnectionCreateResponse data = new UserDbConnectionCreateResponse(UUID.randomUUID());

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userDbConnectionService.create(workspaceId, body)).thenReturn(data);

        ResponseEntity<ApiResponse<UserDbConnectionCreateResponse>> response =
                controller.create(workspaceId, tenantId, body, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(true, response.getBody().success());
        assertEquals(data, response.getBody().data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userDbConnectionService).create(workspaceId, body);
    }

    @Test
    void listUsesResolvedWorkspaceId() {
        UserDbConnectionController controller = new UserDbConnectionController(userDbConnectionService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserDbConnectionListResponse data = new UserDbConnectionListResponse(List.of());

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userDbConnectionService.list(workspaceId)).thenReturn(data);

        ApiResponse<UserDbConnectionListResponse> response = controller.list(workspaceId, tenantId, request);

        assertEquals(true, response.success());
        assertEquals(data, response.data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userDbConnectionService).list(workspaceId);
    }

    @Test
    void testUsesResolvedWorkspaceId() {
        UserDbConnectionController controller = new UserDbConnectionController(userDbConnectionService, workspaceContextResolver);
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        UserDbConnectionTestResponse data = new UserDbConnectionTestResponse(true, "connection successful");

        when(workspaceContextResolver.resolve(workspaceId, tenantId, request)).thenReturn(workspaceId);
        when(userDbConnectionService.test(workspaceId, connectionId)).thenReturn(data);

        ApiResponse<UserDbConnectionTestResponse> response = controller.test(connectionId, workspaceId, tenantId, request);

        assertEquals(true, response.success());
        assertEquals(data, response.data());
        verify(workspaceContextResolver).resolve(workspaceId, tenantId, request);
        verify(userDbConnectionService).test(workspaceId, connectionId);
    }

    @Test
    void createPropagatesWorkspaceRequiredException() {
        UserDbConnectionController controller = new UserDbConnectionController(userDbConnectionService, workspaceContextResolver);
        UserDbConnectionCreateRequest body = new UserDbConnectionCreateRequest(
                "main", "POSTGRESQL", "localhost", 5432, "db", "user", "pw", null, "disable"
        );
        UUID tenantId = UUID.randomUUID();

        when(workspaceContextResolver.resolve(null, tenantId, request))
                .thenThrow(new UserWasException(400, "workspace_id is required"));

        UserWasException exception = assertThrows(UserWasException.class,
                () -> controller.create(null, tenantId, body, request));

        assertEquals(400, exception.getStatus());
        assertEquals("workspace_id is required", exception.getMessage());
    }
}

