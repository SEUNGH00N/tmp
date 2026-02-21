package com.example.excelimport.userwas.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.user-import.dispatch-mode", havingValue = "sync", matchIfMissing = true)
public class LocalAsyncUserImportEventPublisher implements UserImportEventPublisher {

    private final UserImportProcessingService processingService;

    public LocalAsyncUserImportEventPublisher(UserImportProcessingService processingService) {
        this.processingService = processingService;
    }

    @Override
    public void publish(UserImportProcessRequestedEvent event) {
        processingService.processAsync(event.jobId(), event.extension(), event.fileUri());
    }
}
