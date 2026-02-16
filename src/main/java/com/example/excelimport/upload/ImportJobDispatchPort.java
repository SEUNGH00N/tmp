package com.example.excelimport.upload;

import java.util.UUID;

public interface ImportJobDispatchPort {
    UUID createCreatedJob(UUID tenantId, String fileUri);

    void dispatch(UUID jobId, String extension);
}
