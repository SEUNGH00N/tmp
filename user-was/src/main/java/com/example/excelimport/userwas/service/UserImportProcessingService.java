package com.example.excelimport.userwas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    public void processAsync(UUID jobId, String extension, String fileUri) {
        UUID workspaceId = null;
        try {
            workspaceId = lookupWorkspaceId(jobId);
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
            if (workspaceId != null) {
                userBillingService.addProcessingOutcome(workspaceId, processed, fail);
            }
        } catch (Exception ex) {
            jdbcTemplate.update(
                    "update import_job set status = ?, finished_at = now() where id = ?",
                    "FAILED", jobId
            );
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
