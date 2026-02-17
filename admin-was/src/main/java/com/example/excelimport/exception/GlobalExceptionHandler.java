package com.example.excelimport.exception;

import com.example.excelimport.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        pd.setType(URI.create(ex.getType()));
        pd.setTitle(ex.getTitle());
        pd.setInstance(URI.create(request.getRequestURI()));
        if (!ex.getErrors().isEmpty()) pd.setProperty("errors", ex.getErrors());
        pd.setProperty("requestId", requestId(request));
        return ResponseEntity.status(ex.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "https://example.com/problems/bad-request", "Bad Request", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "https://example.com/problems/upload-too-large", "Upload Too Large", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "https://example.com/problems/internal-error", "Internal Server Error", ex.getMessage(), request, List.of());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String type, String title, String detail, HttpServletRequest request, List<Map<String, Object>> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(type));
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        if (!errors.isEmpty()) pd.setProperty("errors", errors);
        pd.setProperty("requestId", requestId(request));
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    private String requestId(HttpServletRequest request) {
        Object v = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return v == null ? "" : v.toString();
    }
}
