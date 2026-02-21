package com.example.excelimport.userwas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.user-import.dispatch-mode", havingValue = "kafka")
public class KafkaReadyUserImportEventPublisher implements UserImportEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaReadyUserImportEventPublisher.class);

    private final UserImportProcessingService processingService;
    private final boolean localFallbackEnabled;

    public KafkaReadyUserImportEventPublisher(
            UserImportProcessingService processingService,
            @Value("${app.user-import.kafka.local-fallback-enabled:true}") boolean localFallbackEnabled
    ) {
        this.processingService = processingService;
        this.localFallbackEnabled = localFallbackEnabled;
    }

    @Override
    public void publish(UserImportProcessRequestedEvent event) {
        // Kafka producer adapter point: replace with KafkaTemplate send(topic, payload)
        log.info("Kafka dispatch-mode enabled. Publish candidate event: jobId={}, runNo={}, extension={}, requestedAt={}",
                event.jobId(), event.runNo(), event.extension(), event.requestedAt());
        if (localFallbackEnabled) {
            processingService.processAsync(event.jobId(), event.runId(), event.runNo(), event.extension(), event.fileUri());
        }
    }
}
