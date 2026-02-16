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

    public ImportWorkerService(ImportJobRepository importJobRepository,
                               ExcelRowDataRepository excelRowDataRepository,
                               ImportErrorLogRepository importErrorLogRepository,
                               List<RowParser> parsers,
                               RowValidator rowValidator,
                               ObjectMapper objectMapper) {
        this.importJobRepository = importJobRepository;
        this.excelRowDataRepository = excelRowDataRepository;
        this.importErrorLogRepository = importErrorLogRepository;
        this.parsers = parsers;
        this.rowValidator = rowValidator;
        this.objectMapper = objectMapper;
    }

    @Async("importTaskExecutor")
    @Transactional
    public void processAsync(UUID jobId, String extension) {
        ImportJob job = importJobRepository.findById(jobId).orElseThrow();
        try {
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
        } catch (Exception e) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setFinishedAt(Instant.now());
            importJobRepository.save(job);
        }
    }

    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}
