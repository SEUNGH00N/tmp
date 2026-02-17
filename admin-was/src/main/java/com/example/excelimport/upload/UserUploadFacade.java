package com.example.excelimport.upload;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class UserUploadFacade {

    private final UploadGuard uploadGuard;
    private final UploadStoragePort uploadStoragePort;
    private final ImportJobDispatchPort importJobDispatchPort;

    public UserUploadFacade(UploadGuard uploadGuard,
                            UploadStoragePort uploadStoragePort,
                            ImportJobDispatchPort importJobDispatchPort) {
        this.uploadGuard = uploadGuard;
        this.uploadStoragePort = uploadStoragePort;
        this.importJobDispatchPort = importJobDispatchPort;
    }

    @Transactional
    public UserUploadResult createImportJob(UUID workspaceId, MultipartFile file) {
        UserUploadCommand command = new UserUploadCommand(workspaceId, file);
        uploadGuard.validate(command);

        UploadStoragePort.UploadStoredFile stored = uploadStoragePort.store(command.workspaceId(), command.file());
        UUID jobId = importJobDispatchPort.createCreatedJob(command.workspaceId(), stored.fileUri());
        importJobDispatchPort.dispatch(jobId, stored.extension());

        return new UserUploadResult(jobId, stored.fileUri(), stored.extension());
    }
}
