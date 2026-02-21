package com.example.excelimport.controller;

import com.example.excelimport.auth.SessionKeys;
import com.example.excelimport.auth.TokenService;
import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.common.web.RequestIdFilter;
import com.example.excelimport.dto.ApprovalDecisionRequest;
import com.example.excelimport.dto.ApprovalRequestItem;
import com.example.excelimport.dto.FeatureFlagItem;
import com.example.excelimport.dto.FeatureFlagUpsertRequest;
import com.example.excelimport.exception.ApiException;
import com.example.excelimport.service.AdminGovernanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminGovernanceController {

    private final AdminGovernanceService adminGovernanceService;
    private final TokenService tokenService;

    public AdminGovernanceController(AdminGovernanceService adminGovernanceService,
                                     TokenService tokenService) {
        this.adminGovernanceService = adminGovernanceService;
        this.tokenService = tokenService;
    }

    @GetMapping("/approvals/requests")
    public ApiResponse<List<ApprovalRequestItem>> listApprovalRequests(
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminGovernanceService.listApprovalRequests(status), requestId(request));
    }

    @PostMapping("/approvals/requests/{requestId}/decision")
    public ApiResponse<Void> decideApproval(
            @PathVariable UUID requestId,
            @Valid @RequestBody ApprovalDecisionRequest body,
            HttpServletRequest request
    ) {
        String actor = actorUsername(request);
        adminGovernanceService.decideApproval(requestId, body.decision(), body.reason(), actor);
        return ApiResponse.success(null, requestId(request));
    }

    @GetMapping("/workspaces/{workspaceId}/feature-flags")
    public ApiResponse<List<FeatureFlagItem>> listFeatureFlags(
            @PathVariable UUID workspaceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminGovernanceService.listFeatureFlags(workspaceId), requestId(request));
    }

    @PutMapping("/workspaces/{workspaceId}/feature-flags/{featureCode}")
    public ApiResponse<FeatureFlagItem> upsertFeatureFlag(
            @PathVariable UUID workspaceId,
            @PathVariable String featureCode,
            @RequestBody FeatureFlagUpsertRequest body,
            HttpServletRequest request
    ) {
        FeatureFlagItem data = adminGovernanceService.upsertFeatureFlag(
                workspaceId,
                featureCode,
                body.enabled(),
                body.configJson()
        );
        return ApiResponse.success(data, requestId(request));
    }

    private String actorUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute(SessionKeys.AUTH_USER);
        if (username != null && !username.isBlank()) {
            return username;
        }
        username = tokenService.resolveUsername(extractBearerToken(request));
        if (username == null || username.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "https://example.com/problems/unauthorized",
                    "Unauthorized",
                    "login required");
        }
        return username;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        return auth.substring("Bearer ".length()).trim();
    }

    private String requestId(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }
}
