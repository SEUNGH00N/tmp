package com.example.excelimport.upload;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
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

        byte[] content = readBytes(command.file());
        long fileSize = content.length;
        String checksum = sha256Hex(content);

        UUID duplicateJobId = importJobDispatchPort.findDuplicateJob(command.workspaceId(), checksum, fileSize);
        if (duplicateJobId != null) {
            return new UserUploadResult(duplicateJobId, "dedup://existing", fileExtension(command.file().getOriginalFilename()));
        }

        UploadStoragePort.UploadStoredFile stored = uploadStoragePort.store(command.workspaceId(), command.file());
        UUID jobId = importJobDispatchPort.createCreatedJob(
                command.workspaceId(),
                stored.fileUri(),
                command.file().getOriginalFilename(),
                fileSize,
                checksum
        );
        importJobDispatchPort.upsertDedup(command.workspaceId(), checksum, fileSize, jobId);
        importJobDispatchPort.dispatch(jobId, stored.extension());

        return new UserUploadResult(jobId, stored.fileUri(), stored.extension());
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new IllegalStateException("failed to read file bytes", e);
        }
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("failed to compute checksum", e);
        }
    }

    private String fileExtension(String name) {
        if (name == null) {
            return "unknown";
        }
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "unknown";
        }
        return name.substring(idx + 1).toLowerCase();
    }
}
