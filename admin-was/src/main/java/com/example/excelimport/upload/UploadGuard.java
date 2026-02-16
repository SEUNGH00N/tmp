package com.example.excelimport.upload;

public interface UploadGuard {
    void validate(UserUploadCommand command);
}
