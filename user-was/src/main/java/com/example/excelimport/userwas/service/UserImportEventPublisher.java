package com.example.excelimport.userwas.service;

public interface UserImportEventPublisher {
    void publish(UserImportProcessRequestedEvent event);
}
