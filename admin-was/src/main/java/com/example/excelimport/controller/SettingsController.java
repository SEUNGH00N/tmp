package com.example.excelimport.controller;

import com.example.excelimport.config.RequestIdFilter;
import com.example.excelimport.dto.ApiResponse;
import com.example.excelimport.dto.SettingsRequest;
import com.example.excelimport.dto.SettingsResponse;
import com.example.excelimport.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ApiResponse<SettingsResponse> get(HttpServletRequest request) {
        return ApiResponse.success(settingsService.get(), requestId(request));
    }

    @PutMapping
    public ApiResponse<SettingsResponse> update(@RequestBody SettingsRequest body, HttpServletRequest request) {
        return ApiResponse.success(settingsService.update(body), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }
}
