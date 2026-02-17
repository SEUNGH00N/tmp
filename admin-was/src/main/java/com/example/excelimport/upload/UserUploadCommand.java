package com.example.excelimport.upload;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record UserUploadCommand(UUID workspaceId, MultipartFile file) {
}
