package com.example.excelimport.userwas.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionCreateRequest;
import com.example.excelimport.userwas.dto.UserDbConnectionCreateResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionListResponse;
import com.example.excelimport.userwas.dto.UserDbConnectionTestResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.service.UserDbConnectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/db-connections")
public class UserDbConnectionController {

    private static final String WS_SESSION_KEY = "USER_WORKSPACE_ID";
    private final UserDbConnectionService userDbConnectionService;

    public UserDbConnectionController(UserDbConnectionService userDbConnectionService) {
        this.userDbConnectionService = userDbConnectionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDbConnectionCreateResponse>> create(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            @RequestBody UserDbConnectionCreateRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, servletRequest);
        UserDbConnectionCreateResponse data = userDbConnectionService.create(effectiveWorkspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    @GetMapping
    public ApiResponse<UserDbConnectionListResponse> list(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest servletRequest
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, servletRequest);
        return ApiResponse.success(userDbConnectionService.list(effectiveWorkspaceId));
    }

    @PostMapping("/{connectionId}/test")
    public ApiResponse<UserDbConnectionTestResponse> test(
            @PathVariable UUID connectionId,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest servletRequest
    ) {
        UUID effectiveWorkspaceId = resolveWorkspaceId(workspaceId, tenantId, servletRequest);
        return ApiResponse.success(userDbConnectionService.test(effectiveWorkspaceId, connectionId));
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
