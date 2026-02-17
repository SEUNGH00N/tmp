package com.example.excelimport.controller;

import com.example.excelimport.common.web.ApiResponse;
import com.example.excelimport.common.web.RequestIdFilter;
import com.example.excelimport.dto.ImportCreateResponse;
import com.example.excelimport.dto.ImportStatusResponse;
import com.example.excelimport.dto.PagedExcelRowsResponse;
import com.example.excelimport.dto.PagedErrorResponse;
import com.example.excelimport.dto.PagedImportJobsResponse;
import com.example.excelimport.exception.ApiException;
import com.example.excelimport.service.ImportJobService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ImportController {

    private final ImportJobService importJobService;

    public ImportController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PostMapping(value = "/imports", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ImportCreateResponse>> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @RequestParam(value = "tenant_id", required = false) UUID tenantId,
            HttpServletRequest request) {
        UUID effectiveWorkspaceId = workspaceId != null ? workspaceId : tenantId;
        if (effectiveWorkspaceId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/missing-parameter",
                    "Bad Request",
                    "workspace_id is required");
        }
        UUID jobId = importJobService.createJob(effectiveWorkspaceId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new ImportCreateResponse(jobId), requestId(request)));
    }

    @GetMapping("/imports")
    public ApiResponse<PagedImportJobsResponse> list(
            @RequestParam(value = "workspace_id", required = false) UUID workspaceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        return ApiResponse.success(importJobService.getJobs(pageable, workspaceId), requestId(request));
    }

    @GetMapping("/imports/{jobId}")
    public ApiResponse<ImportStatusResponse> status(@PathVariable UUID jobId, HttpServletRequest request) {
        ImportStatusResponse data = importJobService.getStatus(jobId);
        return ApiResponse.success(data, requestId(request));
    }

    @GetMapping("/imports/{jobId}/errors")
    public ApiResponse<PagedErrorResponse> errors(
            @PathVariable UUID jobId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        PagedErrorResponse data = importJobService.getErrors(jobId, pageable);
        return ApiResponse.success(data, requestId(request));
    }

    @GetMapping("/imports/{jobId}/rows")
    public ApiResponse<PagedExcelRowsResponse> rows(
            @PathVariable UUID jobId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        PagedExcelRowsResponse data = importJobService.getRows(jobId, pageable);
        return ApiResponse.success(data, requestId(request));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public void handleMissingParam() {
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "https://example.com/problems/missing-parameter",
                "Bad Request",
                "required parameter is missing");
    }

    private String requestId(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return attr == null ? "" : attr.toString();
    }
}
