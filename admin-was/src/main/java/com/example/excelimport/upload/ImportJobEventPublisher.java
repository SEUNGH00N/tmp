package com.example.excelimport.upload;

public interface ImportJobEventPublisher {
    void publish(ImportJobProcessRequestedEvent event);
}
