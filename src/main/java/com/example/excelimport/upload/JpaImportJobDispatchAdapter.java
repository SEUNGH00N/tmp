package com.example.excelimport.upload;

import com.example.excelimport.entity.ImportJob;
import com.example.excelimport.entity.ImportJobStatus;
import com.example.excelimport.repository.ImportJobRepository;
import com.example.excelimport.service.ImportWorkerService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaImportJobDispatchAdapter implements ImportJobDispatchPort {

    private final ImportJobRepository importJobRepository;
    private final ImportWorkerService importWorkerService;

    public JpaImportJobDispatchAdapter(ImportJobRepository importJobRepository,
                                       ImportWorkerService importWorkerService) {
        this.importJobRepository = importJobRepository;
        this.importWorkerService = importWorkerService;
    }

    @Override
    public UUID createCreatedJob(UUID tenantId, String fileUri) {
        ImportJob job = new ImportJob();
        job.setTenantId(tenantId);
        job.setStatus(ImportJobStatus.CREATED);
        job.setFileUri(fileUri);
        job.setTotalRows(0);
        job.setProcessedRows(0);
        job.setSuccessCount(0);
        job.setFailCount(0);
        ImportJob saved = importJobRepository.save(job);
        return saved.getId();
    }

    @Override
    public void dispatch(UUID jobId, String extension) {
        importWorkerService.processAsync(jobId, extension);
    }
}
