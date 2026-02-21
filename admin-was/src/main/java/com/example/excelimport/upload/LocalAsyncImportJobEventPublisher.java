package com.example.excelimport.upload;

import com.example.excelimport.service.ImportWorkerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.import.dispatch-mode", havingValue = "sync", matchIfMissing = true)
public class LocalAsyncImportJobEventPublisher implements ImportJobEventPublisher {

    private final ImportWorkerService importWorkerService;

    public LocalAsyncImportJobEventPublisher(ImportWorkerService importWorkerService) {
        this.importWorkerService = importWorkerService;
    }

    @Override
    public void publish(ImportJobProcessRequestedEvent event) {
        importWorkerService.processAsync(event.jobId(), event.runId(), event.runNo(), event.extension());
    }
}
