package com.example.excelimport.userwas.service;

import com.example.excelimport.userwas.dto.UserImportCreateResponse;
import com.example.excelimport.userwas.dto.UserImportListItem;
import com.example.excelimport.userwas.dto.UserImportListResponse;
import com.example.excelimport.userwas.dto.UserImportStatusResponse;
import com.example.excelimport.userwas.exception.UserWasException;
import com.example.excelimport.userwas.storage.UserFileStorageService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserImportService {

    private final JdbcTemplate jdbcTemplate;
    private final UserFileStorageService storageService;

    public UserImportService(JdbcTemplate jdbcTemplate, UserFileStorageService storageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
    }

    public UserImportCreateResponse create(UUID tenantId, MultipartFile file) {
        if (tenantId == null) throw new UserWasException(400, "tenant_id is required");
        if (file == null || file.isEmpty()) throw new UserWasException(400, "file is required");

        UserFileStorageService.StoredFile stored;
        try {
            stored = storageService.store(tenantId, file);
        } catch (IOException e) {
            throw new UserWasException(500, e.getMessage());
        }

        UUID jobId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into import_job (id, tenant_id, status, file_uri, total_rows, processed_rows, success_count, fail_count, error_log_uri, created_at, started_at, finished_at) values (?, ?, ?, ?, 0, 0, 0, 0, null, now(), null, null)",
                jobId, tenantId, "CREATED", stored.fileUri()
        );

        // Skeleton: publish event/queue for admin processing worker.
        return new UserImportCreateResponse(jobId);
    }

    public UserImportListResponse list(UUID tenantId, int page, int size) {
        if (tenantId == null) throw new UserWasException(400, "tenant_id is required");
        if (size < 1) size = 20;
        if (page < 0) page = 0;

        Integer total = jdbcTemplate.queryForObject("select count(*) from import_job where tenant_id = ?", Integer.class, tenantId);
        int totalElements = total == null ? 0 : total;

        List<UserImportListItem> items = jdbcTemplate.query(
                "select id, tenant_id, status, total_rows, processed_rows, created_at from import_job where tenant_id = ? order by created_at desc limit ? offset ?",
                new ImportListMapper(), tenantId, size, page * size
        );

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new UserImportListResponse(items, page, size, totalElements, totalPages);
    }

    public UserImportStatusResponse status(UUID tenantId, UUID jobId) {
        if (tenantId == null) throw new UserWasException(400, "tenant_id is required");

        List<UserImportStatusResponse> rows = jdbcTemplate.query(
                "select id, status, total_rows, processed_rows from import_job where id = ? and tenant_id = ?",
                (rs, n) -> {
                    int totalRows = rs.getInt("total_rows");
                    int processedRows = rs.getInt("processed_rows");
                    int pct = totalRows == 0 ? 0 : (int) Math.floor((processedRows * 100.0) / totalRows);
                    return new UserImportStatusResponse(
                            (UUID) rs.getObject("id"),
                            rs.getString("status"),
                            pct,
                            totalRows,
                            processedRows
                    );
                },
                jobId, tenantId
        );

        if (rows.isEmpty()) throw new UserWasException(404, "job not found");
        return rows.get(0);
    }

    private static class ImportListMapper implements RowMapper<UserImportListItem> {
        @Override
        public UserImportListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            int totalRows = rs.getInt("total_rows");
            int processedRows = rs.getInt("processed_rows");
            int pct = totalRows == 0 ? 0 : (int) Math.floor((processedRows * 100.0) / totalRows);
            Instant createdAt = rs.getTimestamp("created_at").toInstant();
            return new UserImportListItem(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("tenant_id"),
                    rs.getString("status"),
                    pct,
                    totalRows,
                    processedRows,
                    createdAt
            );
        }
    }
}
