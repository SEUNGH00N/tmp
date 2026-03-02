package com.example.excelimport.userwas.exception;

import com.example.excelimport.common.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserWasExceptionHandlerTest {

    private final UserWasExceptionHandler handler = new UserWasExceptionHandler();

    @Test
    void handleAddsTypeTitleAndRequestIdForClientError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/user/imports");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTR, "req-001");
        UserWasException ex = new UserWasException(400, "workspace_id is required");

        ResponseEntity<ProblemDetail> response = handler.handle(ex, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("workspace_id is required", response.getBody().getDetail());
        assertEquals("Bad Request", response.getBody().getTitle());
        assertEquals("https://example.com/problems/user-bad-request", response.getBody().getType().toString());
        assertEquals("/api/v1/user/imports", response.getBody().getInstance().toString());
        assertEquals("req-001", response.getBody().getProperties().get("requestId"));
    }

    @Test
    void handleAddsServerTypeForServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/user/imports");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTR, "req-500");
        UserWasException ex = new UserWasException(500, "failed");

        ResponseEntity<ProblemDetail> response = handler.handle(ex, request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal Server Error", response.getBody().getTitle());
        assertEquals("https://example.com/problems/user-internal-error", response.getBody().getType().toString());
        assertEquals("req-500", response.getBody().getProperties().get("requestId"));
    }
}

