package com.example.excelimport.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.common.web.RequestIdFilter;
import com.example.excelimport.dto.PagedAppUsersResponse;
import com.example.excelimport.service.AppUserQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final AppUserQueryService appUserQueryService;

    public UserController(AppUserQueryService appUserQueryService) {
        this.appUserQueryService = appUserQueryService;
    }

    @GetMapping("/users")
    public ApiResponse<PagedAppUsersResponse> users(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        return ApiResponse.success(appUserQueryService.getUsers(pageable), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }
}
