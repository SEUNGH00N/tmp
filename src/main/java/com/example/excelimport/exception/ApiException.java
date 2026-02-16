package com.example.excelimport.exception;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String title;
    private final List<Map<String, Object>> errors;

    public ApiException(HttpStatus status, String type, String title, String detail) {
        this(status, type, title, detail, List.of());
    }

    public ApiException(HttpStatus status, String type, String title, String detail, List<Map<String, Object>> errors) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
        this.errors = errors;
    }

    public HttpStatus getStatus() { return status; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public List<Map<String, Object>> getErrors() { return errors; }
}
