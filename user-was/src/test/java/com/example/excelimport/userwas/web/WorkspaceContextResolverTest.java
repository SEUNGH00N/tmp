package com.example.excelimport.userwas.web;

import com.example.excelimport.userwas.exception.UserWasException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspaceContextResolverTest {

    private final WorkspaceContextResolver resolver = new WorkspaceContextResolver();

    @Test
    void resolveReturnsWorkspaceIdWhenWorkspaceIdProvided() {
        UUID workspaceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();

        UUID result = resolver.resolve(workspaceId, tenantId, request);

        assertEquals(workspaceId, result);
        assertEquals(workspaceId.toString(),
                request.getSession(false).getAttribute(WorkspaceContextResolver.WS_SESSION_KEY));
    }

    @Test
    void resolveReturnsTenantIdWhenWorkspaceIdMissing() {
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();

        UUID result = resolver.resolve(null, tenantId, request);

        assertEquals(tenantId, result);
        assertEquals(tenantId.toString(),
                request.getSession(false).getAttribute(WorkspaceContextResolver.WS_SESSION_KEY));
    }

    @Test
    void resolveReturnsSessionValueWhenParamsMissing() {
        UUID workspaceId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(WorkspaceContextResolver.WS_SESSION_KEY, workspaceId.toString());

        UUID result = resolver.resolve(null, null, request);

        assertEquals(workspaceId, result);
    }

    @Test
    void resolveThrows400WhenWorkspaceContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        UserWasException exception = assertThrows(UserWasException.class,
                () -> resolver.resolve(null, null, request));

        assertEquals(400, exception.getStatus());
        assertEquals("workspace_id is required", exception.getMessage());
    }
}

