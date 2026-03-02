package com.example.excelimport.userwas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserImportProcessingService {
    private static final Logger log = LoggerFactory.getLogger(UserImportProcessingService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserBillingService userBillingService;

    public UserImportProcessingService(JdbcTemplate jdbcTemplate,
                                       ObjectMapper objectMapper,
                                       UserBillingService userBillingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.userBillingService = userBillingService;
    }

    @Async("userImportExecutor")
    public void processAsync(UUID jobId, UUID runId, int runNo, String extension, String fileUri) {
        UUID workspaceId = null;
        try {
            workspaceId = lookupWorkspaceId(jobId);
            markRunRunning(runId);
            addEvent(jobId, "RUN_STARTED", "INFO", "{\"runNo\":" + runNo + "}");
            updateStatus(jobId, "PARSING");
            jdbcTemplate.update("update import_job set started_at = now() where id = ?", jobId);

            List<Map<String, String>> rows = parse(extension, fileUri);

            jdbcTemplate.update(
                    "update import_job set status = ?, total_rows = ? where id = ?",
                    "VALIDATING", rows.size(), jobId
            );
            updateStatus(jobId, "LOADING");

            int processed = 0;
            int success = 0;
            int fail = 0;

            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);
                int rowIndex = i + 1;

                List<String[]> errors = validate(row);
                if (errors.isEmpty()) {
                    String payload = objectMapper.writeValueAsString(row);
                    jdbcTemplate.update(
                            "insert into excel_data (id, job_id, payload_json, created_at) values (?, ?, ?::jsonb, now())",
                            UUID.randomUUID(), jobId, payload
                    );
                    success++;
                } else {
                    fail++;
                    for (String[] e : errors) {
                        jdbcTemplate.update(
                                "insert into error_log (id, job_id, row_index, column_name, error_code, error_msg, created_at) values (?, ?, ?, ?, ?, ?, now())",
                                UUID.randomUUID(), jobId, rowIndex, e[0], e[1], e[2]
                        );
                    }
                }
                processed++;
            }

            jdbcTemplate.update(
                    "update import_job set status = ?, processed_rows = ?, success_count = ?, fail_count = ?, finished_at = now() where id = ?",
                    "COMPLETED", processed, success, fail, jobId
            );
            markRunCompleted(runId);
            addMetric(jobId, "run_no", runNo);
            addMetric(jobId, "success_count", success);
            addMetric(jobId, "fail_count", fail);
            addMetric(jobId, "processed_rows", processed);
            addEvent(jobId, "RUN_COMPLETED", "INFO",
                    "{\"runNo\":" + runNo + ",\"processed\":" + processed + ",\"success\":" + success + ",\"failed\":" + fail + "}");
            if (workspaceId != null) {
                userBillingService.addProcessingOutcome(workspaceId, processed, fail);
            }
        } catch (Exception ex) {
            log.error(
                    "User import worker failed. jobId={}, runId={}, runNo={}, exceptionType={}, message={}",
                    jobId,
                    runId,
                    runNo,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );
            jdbcTemplate.update(
                    "update import_job set status = ?, finished_at = now() where id = ?",
                    "FAILED", jobId
            );
            markRunFailed(runId, ex.getMessage());
            addEvent(jobId, "RUN_FAILED", "ERROR",
                    "{\"runNo\":" + runNo + ",\"message\":\"" + escapeJson(ex.getMessage()) + "\"}");
        }
    }

    private UUID lookupWorkspaceId(UUID jobId) {
        return jdbcTemplate.query(
                "select workspace_id from import_job where id = ?",
                rs -> rs.next() ? (UUID) rs.getObject("workspace_id") : null,
                jobId
        );
    }

    private void updateStatus(UUID jobId, String status) {
        jdbcTemplate.update(
                "update import_job set status = ? where id = ?",
                status, jobId
        );
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

    private List<String[]> validate(Map<String, String> row) {
        List<String[]> errors = new ArrayList<>();
        String col1 = row.getOrDefault("col_1", "");
        if (col1 == null || col1.trim().isEmpty()) {
            errors.add(new String[]{"col_1", "IMP-VAL-001", "col_1 is required"});
        }
        for (Map.Entry<String, String> e : row.entrySet()) {
            String v = e.getValue();
            if (v != null && v.length() > 255) {
                errors.add(new String[]{e.getKey(), "IMP-VAL-002", "cell length must be <= 255"});
            }
        }
        return errors;
    }

    private List<Map<String, String>> parse(String extension, String fileUri) throws Exception {
        if ("csv".equalsIgnoreCase(extension)) {
            return parseCsv(fileUri);
        }
        if ("xlsx".equalsIgnoreCase(extension)) {
            return parseXlsx(fileUri);
        }
        throw new IllegalArgumentException("unsupported extension: " + extension);
    }

    private List<Map<String, String>> parseCsv(String fileUri) throws Exception {
        List<Map<String, String>> out = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Path.of(fileUri), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);
            for (CSVRecord r : records) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < r.size(); i++) {
                    row.put("col_" + (i + 1), r.get(i));
                }
                out.add(row);
            }
        }
        return out;
    }

    private List<Map<String, String>> parseXlsx(String fileUri) throws Exception {
        List<Map<String, String>> out = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream in = Files.newInputStream(Path.of(fileUri)); XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row r : sheet) {
                int last = Math.max(0, r.getLastCellNum());
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < last; i++) {
                    row.put("col_" + (i + 1), formatter.formatCellValue(r.getCell(i)));
                }
                out.add(row);
            }
        }
        return out;
    }
}
