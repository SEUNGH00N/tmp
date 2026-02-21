package com.example.excelimport.upload;

import com.example.excelimport.service.ImportWorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.import.dispatch-mode", havingValue = "kafka")
public class KafkaReadyImportJobEventPublisher implements ImportJobEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaReadyImportJobEventPublisher.class);

    private final ImportWorkerService importWorkerService;
    private final boolean localFallbackEnabled;

    public KafkaReadyImportJobEventPublisher(ImportWorkerService importWorkerService,
                                             @Value("${app.import.kafka.local-fallback-enabled:true}") boolean localFallbackEnabled) {
        this.importWorkerService = importWorkerService;
        this.localFallbackEnabled = localFallbackEnabled;
    }

    @Override
    public void publish(ImportJobProcessRequestedEvent event) {
        // Kafka producer adapter point: replace with KafkaTemplate send(topic, payload)
        log.info("Kafka dispatch-mode enabled. Publish candidate event: jobId={}, runNo={}, extension={}, requestedAt={}",
                event.jobId(), event.runNo(), event.extension(), event.requestedAt());
        if (localFallbackEnabled) {
            importWorkerService.processAsync(event.jobId(), event.runId(), event.runNo(), event.extension());
        }
    }
}
