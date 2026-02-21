package com.example.excelimport.upload;

import com.example.excelimport.entity.ImportJob;
import com.example.excelimport.entity.ImportJobStatus;
import com.example.excelimport.repository.ImportJobRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaImportJobDispatchAdapter implements ImportJobDispatchPort {

    private final ImportJobRepository importJobRepository;
    private final ImportJobEventPublisher importJobEventPublisher;

    public JpaImportJobDispatchAdapter(ImportJobRepository importJobRepository,
                                       ImportJobEventPublisher importJobEventPublisher) {
        this.importJobRepository = importJobRepository;
        this.importJobEventPublisher = importJobEventPublisher;
    }

    @Override
    public UUID createCreatedJob(UUID workspaceId, String fileUri) {
        ImportJob job = new ImportJob();
        job.setTenantId(workspaceId); // transitional compatibility
        job.setWorkspaceId(workspaceId);
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
        importJobEventPublisher.publish(new ImportJobProcessRequestedEvent(jobId, extension, Instant.now()));
    }
}
