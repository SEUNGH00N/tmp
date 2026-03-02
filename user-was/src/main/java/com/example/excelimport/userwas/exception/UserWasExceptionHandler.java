package com.example.excelimport.userwas.exception;

import com.example.excelimport.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class UserWasExceptionHandler {

    @ExceptionHandler(UserWasException.class)
    public ResponseEntity<ProblemDetail> handle(UserWasException ex, HttpServletRequest request) {
        HttpStatusCode status = HttpStatusCode.valueOf(ex.getStatus());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setType(URI.create(type(ex.getStatus())));
        pd.setTitle(title(ex.getStatus()));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("requestId", requestId(request));
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private String requestId(HttpServletRequest request) {
        Object v = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return v == null ? "" : v.toString();
    }

    private String title(int status) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        if (httpStatus != null) {
            return httpStatus.getReasonPhrase();
        }
        return "Error";
    }

    private String type(int status) {
        if (status >= 400 && status < 500) {
            return "https://example.com/problems/user-bad-request";
        }
        if (status >= 500) {
            return "https://example.com/problems/user-internal-error";
        }
        return "https://example.com/problems/user-error";
    }
}
