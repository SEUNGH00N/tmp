package com.example.excelimport.userwas.exception;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserWasExceptionHandler {

    @ExceptionHandler(UserWasException.class)
    public ResponseEntity<ProblemDetail> handle(UserWasException ex) {
        HttpStatusCode status = HttpStatusCode.valueOf(ex.getStatus());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
