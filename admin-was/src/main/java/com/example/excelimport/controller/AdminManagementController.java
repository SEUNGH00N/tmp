package com.example.excelimport.controller;

import com.example.excelimport.auth.SessionKeys;
import com.example.excelimport.common.web.RequestIdFilter;
import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.dto.AdminRoleItem;
import com.example.excelimport.dto.AdminUserItem;
import com.example.excelimport.dto.PagedAdminUsersResponse;
import com.example.excelimport.dto.UpdateAdminUserRolesRequest;
import com.example.excelimport.dto.UpdateAdminUserStatusRequest;
import com.example.excelimport.exception.ApiException;
import com.example.excelimport.service.AdminUserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping("/users")
    public ApiResponse<PagedAdminUsersResponse> users(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        return ApiResponse.success(adminUserManagementService.getUsers(pageable), requestId(request));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<AdminUserItem> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateAdminUserStatusRequest body,
            HttpServletRequest request) {
        String actor = actorUsername(request);
        AdminUserItem data = adminUserManagementService.updateUserStatus(userId, body.active(), actor);
        return ApiResponse.success(data, requestId(request));
    }

    @PutMapping("/users/{userId}/roles")
    public ApiResponse<AdminUserItem> updateRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateAdminUserRolesRequest body,
            HttpServletRequest request) {
        String actor = actorUsername(request);
        AdminUserItem data = adminUserManagementService.updateUserRoles(userId, body.roleCodes(), actor);
        return ApiResponse.success(data, requestId(request));
    }

    @GetMapping("/roles")
    public ApiResponse<List<AdminRoleItem>> roles(HttpServletRequest request) {
        return ApiResponse.success(adminUserManagementService.getRoles(), requestId(request));
    }

    private String actorUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute(SessionKeys.AUTH_USER);
        if (username == null || username.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "https://example.com/problems/unauthorized",
                    "Unauthorized",
                    "login required");
        }
        return username;
    }

    private String requestId(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }
}
