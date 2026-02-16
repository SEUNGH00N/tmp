package com.example.excelimport.upload;

import com.example.excelimport.storage.FileStorageService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
public class LocalUploadStorageAdapter implements UploadStoragePort {

    private final FileStorageService fileStorageService;

    public LocalUploadStorageAdapter(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public UploadStoredFile store(UUID tenantId, MultipartFile file) {
        FileStorageService.StoredFile storedFile = fileStorageService.store(file, tenantId);
        return new UploadStoredFile(storedFile.fileUri(), storedFile.extension());
    }
}
