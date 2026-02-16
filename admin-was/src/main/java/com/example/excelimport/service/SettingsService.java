package com.example.excelimport.service;

import com.example.excelimport.dto.SettingsRequest;
import com.example.excelimport.dto.SettingsResponse;
import com.example.excelimport.exception.ApiException;
import com.example.excelimport.storage.StorageProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private volatile String storageRoot;
    private volatile int retentionDays;

    public SettingsService(StorageProperties properties) {
        this.storageRoot = properties.getRoot();
        this.retentionDays = properties.getRetentionDays();
    }

    public SettingsResponse get() {
        return new SettingsResponse(storageRoot, retentionDays);
    }

    public SettingsResponse update(SettingsRequest request) {
        if (request == null) {
            return get();
        }

        if (request.storageRoot() != null && !request.storageRoot().isBlank()) {
            this.storageRoot = request.storageRoot().trim();
        }

        if (request.retentionDays() != null) {
            if (request.retentionDays() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "https://example.com/problems/invalid-settings",
                        "Bad Request",
                        "retentionDays must be greater than 0");
            }
            this.retentionDays = request.retentionDays();
        }

        return get();
    }
}
