package com.example.excelimport.service;

import com.example.excelimport.entity.ExcelRowData;
import com.example.excelimport.entity.ImportErrorLog;
import com.example.excelimport.entity.ImportJob;
import com.example.excelimport.entity.ImportJobStatus;
import com.example.excelimport.parser.RowData;
import com.example.excelimport.parser.RowParser;
import com.example.excelimport.repository.ExcelRowDataRepository;
import com.example.excelimport.repository.ImportErrorLogRepository;
import com.example.excelimport.repository.ImportJobRepository;
import com.example.excelimport.validation.RowValidator;
import com.example.excelimport.validation.ValidationError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ImportWorkerService {

    private final ImportJobRepository importJobRepository;
    private final ExcelRowDataRepository excelRowDataRepository;
    private final ImportErrorLogRepository importErrorLogRepository;
    private final List<RowParser> parsers;
    private final RowValidator rowValidator;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public ImportWorkerService(ImportJobRepository importJobRepository,
                               ExcelRowDataRepository excelRowDataRepository,
                               ImportErrorLogRepository importErrorLogRepository,
                               List<RowParser> parsers,
                               RowValidator rowValidator,
                               ObjectMapper objectMapper,
                               JdbcTemplate jdbcTemplate) {
        this.importJobRepository = importJobRepository;
        this.excelRowDataRepository = excelRowDataRepository;
        this.importErrorLogRepository = importErrorLogRepository;
        this.parsers = parsers;
        this.rowValidator = rowValidator;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Async("importTaskExecutor")
    @Transactional
    public void processAsync(UUID jobId, UUID runId, int runNo, String extension) {
        ImportJob job = importJobRepository.findById(jobId).orElseThrow();
        try {
            markRunRunning(runId);
            addEvent(jobId, "RUN_STARTED", "INFO", "{\"runNo\":" + runNo + "}");

            job.setStatus(ImportJobStatus.PARSING);
            job.setStartedAt(Instant.now());
            importJobRepository.save(job);

            RowParser parser = parsers.stream()
                    .filter(it -> it.supports(extension))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No parser for extension: " + extension));

            List<RowData> rows = parser.parse(Path.of(job.getFileUri()));
            job.setStatus(ImportJobStatus.VALIDATING);
            job.setTotalRows(rows.size());
            importJobRepository.save(job);

            job.setStatus(ImportJobStatus.LOADING);
            int successRows = 0;
            int failedRows = 0;

            for (RowData row : rows) {
                List<ValidationError> errors = rowValidator.validate(row);
                if (errors.isEmpty()) {
                    ExcelRowData data = new ExcelRowData();
                    data.setJob(job);
                    data.setPayloadJson(toJson(row.values()));
                    excelRowDataRepository.save(data);
                    successRows++;
                } else {
                    failedRows++;
                    for (ValidationError error : errors) {
                        ImportErrorLog log = new ImportErrorLog();
                        log.setJob(job);
                        log.setRowIndex(error.rowIndex());
                        log.setColumnName(error.column());
                        log.setErrorCode(error.code());
                        log.setErrorMsg(error.message());
                        importErrorLogRepository.save(log);
                    }
                }
                job.setProcessedRows(job.getProcessedRows() + 1);
                job.setSuccessCount(successRows);
                job.setFailCount(failedRows);
            }

            job.setStatus(ImportJobStatus.COMPLETED);
            job.setFinishedAt(Instant.now());
            importJobRepository.save(job);
            markRunCompleted(runId);
            addMetric(jobId, "run_no", runNo);
            addMetric(jobId, "success_count", successRows);
            addMetric(jobId, "fail_count", failedRows);
            addMetric(jobId, "processed_rows", job.getProcessedRows());
            addEvent(jobId, "RUN_COMPLETED", "INFO",
                    "{\"runNo\":" + runNo + ",\"processed\":" + job.getProcessedRows() + ",\"success\":" + successRows + ",\"failed\":" + failedRows + "}");
        } catch (Exception e) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setFinishedAt(Instant.now());
            importJobRepository.save(job);
            markRunFailed(runId, e.getMessage());
            addEvent(jobId, "RUN_FAILED", "ERROR",
                    "{\"runNo\":" + runNo + ",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private void markRunRunning(UUID runId) {
        jdbcTemplate.update(
                "update import_job_run set status = 'RUNNING', started_at = now(), updated_at = now() where id = ?",
                runId
        );
    }

    private void markRunCompleted(UUID runId) {
        jdbcTemplate.update(
                "update import_job_run set status = 'COMPLETED', finished_at = now(), updated_at = now() where id = ?",
                runId
        );
    }

    private void markRunFailed(UUID runId, String error) {
        jdbcTemplate.update(
                "update import_job_run set status = 'FAILED', finished_at = now(), error = ?, updated_at = now() where id = ?",
                truncate(error, 2000), runId
        );
    }

    private void addEvent(UUID jobId, String type, String level, String payloadJson) {
        jdbcTemplate.update(
                "insert into import_job_event (id, job_id, event_type, level, payload_json, created_at) values (?, ?, ?, ?, ?::jsonb, now())",
                UUID.randomUUID(), jobId, type, level, payloadJson
        );
    }

    private void addMetric(UUID jobId, String key, double value) {
        jdbcTemplate.update(
                "insert into import_job_metric (id, job_id, metric_key, metric_value, created_at) values (?, ?, ?, ?, now())",
                UUID.randomUUID(), jobId, key, value
        );
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
