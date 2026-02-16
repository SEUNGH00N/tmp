package com.example.excelimport.service;

import com.example.excelimport.dto.ImportErrorItem;
import com.example.excelimport.dto.ImportJobListItem;
import com.example.excelimport.dto.ImportStatusResponse;
import com.example.excelimport.dto.ExcelRowItem;
import com.example.excelimport.dto.PagedExcelRowsResponse;
import com.example.excelimport.dto.PagedErrorResponse;
import com.example.excelimport.dto.PagedImportJobsResponse;
import com.example.excelimport.entity.ImportJob;
import com.example.excelimport.entity.ImportJobStatus;
import com.example.excelimport.exception.ApiException;
import com.example.excelimport.repository.ExcelRowDataRepository;
import com.example.excelimport.repository.ImportErrorLogRepository;
import com.example.excelimport.repository.ImportJobRepository;
import com.example.excelimport.storage.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class ImportJobService {

    private final ImportJobRepository importJobRepository;
    private final ImportErrorLogRepository importErrorLogRepository;
    private final ExcelRowDataRepository excelRowDataRepository;
    private final FileStorageService fileStorageService;
    private final ImportWorkerService importWorkerService;

    public ImportJobService(ImportJobRepository importJobRepository,
                            ImportErrorLogRepository importErrorLogRepository,
                            ExcelRowDataRepository excelRowDataRepository,
                            FileStorageService fileStorageService,
                            ImportWorkerService importWorkerService) {
        this.importJobRepository = importJobRepository;
        this.importErrorLogRepository = importErrorLogRepository;
        this.excelRowDataRepository = excelRowDataRepository;
        this.fileStorageService = fileStorageService;
        this.importWorkerService = importWorkerService;
    }

    @Transactional
    public UUID createJob(UUID tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/missing-file",
                    "Bad Request",
                    "file is required");
        }

        FileStorageService.StoredFile storedFile = fileStorageService.store(file, tenantId);

        ImportJob job = new ImportJob();
        job.setTenantId(tenantId);
        job.setStatus(ImportJobStatus.CREATED);
        job.setFileUri(storedFile.fileUri());
        job.setTotalRows(0);
        job.setProcessedRows(0);
        job.setSuccessCount(0);
        job.setFailCount(0);

        ImportJob saved = importJobRepository.save(job);
        importWorkerService.processAsync(saved.getId(), storedFile.extension());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public ImportStatusResponse getStatus(UUID jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> notFound(jobId));
        int progressPct = progress(job.getProcessedRows(), job.getTotalRows());
        return new ImportStatusResponse(job.getId(), job.getStatus(), progressPct, job.getTotalRows(), job.getProcessedRows());
    }

    @Transactional(readOnly = true)
    public PagedImportJobsResponse getJobs(Pageable pageable) {
        Page<ImportJobListItem> page = importJobRepository.findAll(pageable)
                .map(job -> new ImportJobListItem(
                        job.getId(),
                        job.getTenantId(),
                        job.getStatus(),
                        progress(job.getProcessedRows(), job.getTotalRows()),
                        job.getTotalRows(),
                        job.getProcessedRows(),
                        job.getSuccessCount(),
                        job.getFailCount(),
                        job.getCreatedAt()
                ));
        return new PagedImportJobsResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PagedErrorResponse getErrors(UUID jobId, Pageable pageable) {
        if (!importJobRepository.existsById(jobId)) {
            throw notFound(jobId);
        }
        Page<ImportErrorItem> page = importErrorLogRepository.findByJobId(jobId, pageable)
                .map(it -> new ImportErrorItem(it.getRowIndex(), it.getColumnName(), it.getErrorCode(), it.getErrorMsg()));
        return new PagedErrorResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PagedExcelRowsResponse getRows(UUID jobId, Pageable pageable) {
        if (!importJobRepository.existsById(jobId)) {
            throw notFound(jobId);
        }
        Page<ExcelRowItem> page = excelRowDataRepository.findByJobId(jobId, pageable)
                .map(it -> new ExcelRowItem(it.getId(), it.getPayloadJson(), it.getCreatedAt()));
        return new PagedExcelRowsResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private int progress(int processedRows, int totalRows) {
        return totalRows == 0 ? 0 : (int) Math.floor((processedRows * 100.0) / totalRows);
    }

    private ApiException notFound(UUID jobId) {
        return new ApiException(HttpStatus.NOT_FOUND,
                "https://example.com/problems/job-not-found",
                "Not Found",
                "job not found: " + jobId);
    }
}
