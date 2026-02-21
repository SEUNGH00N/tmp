package com.example.excelimport.upload;

import java.util.UUID;

public interface ImportJobDispatchPort {
    UUID createCreatedJob(UUID workspaceId,
                          String fileUri,
                          String originalFilename,
                          long fileSize,
                          String checksum);

    void dispatch(UUID jobId, String extension);

    UUID findDuplicateJob(UUID workspaceId, String checksum, long fileSize);

    void upsertDedup(UUID workspaceId, String checksum, long fileSize, UUID latestJobId);
}
