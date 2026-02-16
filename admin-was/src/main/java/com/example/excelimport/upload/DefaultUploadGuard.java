package com.example.excelimport.upload;

import com.example.excelimport.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DefaultUploadGuard implements UploadGuard {

    @Override
    public void validate(UserUploadCommand command) {
        if (command == null || command.tenantId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/missing-tenant",
                    "Bad Request",
                    "tenant_id is required");
        }

        if (command.file() == null || command.file().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "https://example.com/problems/missing-file",
                    "Bad Request",
                    "file is required");
        }

        // Skeleton: place quota/antivirus/content-signature checks here.
    }
}
