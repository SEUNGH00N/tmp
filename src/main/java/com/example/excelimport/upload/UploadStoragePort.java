package com.example.excelimport.upload;

import java.util.UUID;

public interface UploadStoragePort {
    UploadStoredFile store(UUID tenantId, org.springframework.web.multipart.MultipartFile file);

    record UploadStoredFile(String fileUri, String extension) {
    }
}
