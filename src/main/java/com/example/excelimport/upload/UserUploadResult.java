package com.example.excelimport.upload;

import java.util.UUID;

public record UserUploadResult(UUID jobId, String fileUri, String extension) {
}
