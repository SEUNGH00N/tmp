package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.service.UserImportService;
import com.example.excelimport.userwas.web.RequestIdResolver;
import com.example.excelimport.userwas.web.WorkspaceContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportControllerTest {

    @Mock
    private UserImportService userImportService;

    @Mock
    private WorkspaceContextResolver workspaceContextResolver;

    @Mock
    private RequestIdResolver requestIdResolver;

    @Mock
    private HttpServletRequest request;

    @Test
    void setWorkspaceSessionReturnsMetaRequestId() {
        UserImportController controller = new UserImportController(userImportService, workspaceContextResolver, requestIdResolver);
        UUID workspaceId = UUID.randomUUID();

        when(requestIdResolver.resolve(request)).thenReturn("req-import-session-set");

        ApiResponse<Void> response = controller.setWorkspaceSession(workspaceId, request);

        assertEquals(true, response.success());
        assertEquals("req-import-session-set", response.meta().requestId());
        verify(workspaceContextResolver).setWorkspaceSession(request, workspaceId);
        verify(requestIdResolver).resolve(request);
    }

    @Test
    void getWorkspaceSessionReturnsMetaRequestId() {
        UserImportController controller = new UserImportController(userImportService, workspaceContextResolver, requestIdResolver);

        when(workspaceContextResolver.getWorkspaceSession(request)).thenReturn("ws-1");
        when(requestIdResolver.resolve(request)).thenReturn("req-import-session-get");

        ApiResponse<String> response = controller.getWorkspaceSession(request);

        assertEquals(true, response.success());
        assertEquals("ws-1", response.data());
        assertEquals("req-import-session-get", response.meta().requestId());
        verify(workspaceContextResolver).getWorkspaceSession(request);
        verify(requestIdResolver).resolve(request);
    }
}
