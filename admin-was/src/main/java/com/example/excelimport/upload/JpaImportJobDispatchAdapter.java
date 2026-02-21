package com.example.excelimport.upload;

import com.example.excelimport.entity.ImportJob;
import com.example.excelimport.entity.ImportJobStatus;
import com.example.excelimport.repository.ImportJobRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaImportJobDispatchAdapter implements ImportJobDispatchPort {

    private final ImportJobRepository importJobRepository;
    private final ImportJobEventPublisher importJobEventPublisher;
    private final JdbcTemplate jdbcTemplate;

    public JpaImportJobDispatchAdapter(ImportJobRepository importJobRepository,
                                       ImportJobEventPublisher importJobEventPublisher,
                                       JdbcTemplate jdbcTemplate) {
        this.importJobRepository = importJobRepository;
        this.importJobEventPublisher = importJobEventPublisher;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UUID createCreatedJob(UUID workspaceId,
                                 String fileUri,
                                 String originalFilename,
                                 long fileSize,
                                 String checksum) {
        ImportJob job = new ImportJob();
        job.setTenantId(workspaceId); // transitional compatibility
        job.setWorkspaceId(workspaceId);
        job.setStatus(ImportJobStatus.CREATED);
        job.setFileUri(fileUri);
        job.setOriginalFilename(originalFilename);
        job.setFileSize(fileSize);
        job.setChecksum(checksum);
        job.setTotalRows(0);
        job.setProcessedRows(0);
        job.setSuccessCount(0);
        job.setFailCount(0);
        ImportJob saved = importJobRepository.save(job);
        return saved.getId();
    }

    @Override
    public void dispatch(UUID jobId, String extension) {
        Integer nextRunNo = jdbcTemplate.queryForObject(
                "select coalesce(max(run_no), 0) + 1 from import_job_run where job_id = ?",
                Integer.class,
                jobId
        );
        int runNo = nextRunNo == null ? 1 : nextRunNo;
        UUID runId = UUID.randomUUID();

        jdbcTemplate.update(
                "insert into import_job_run (id, job_id, run_no, worker_id, status, created_at, updated_at) " +
                        "values (?, ?, ?, ?, 'QUEUED', now(), now())",
                runId, jobId, runNo, "admin-local"
        );
        jdbcTemplate.update(
                "insert into import_job_event (id, job_id, event_type, level, payload_json, created_at) " +
                        "values (?, ?, 'JOB_DISPATCHED', 'INFO', ?::jsonb, now())",
                UUID.randomUUID(), jobId, "{\"runNo\":" + runNo + ",\"source\":\"admin\"}"
        );

        importJobEventPublisher.publish(new ImportJobProcessRequestedEvent(jobId, runId, runNo, extension, Instant.now()));
    }

    @Override
    public UUID findDuplicateJob(UUID workspaceId, String checksum, long fileSize) {
        return jdbcTemplate.query(
                "select latest_job_id from import_dedup where workspace_id = ? and checksum = ? and file_size = ?",
                rs -> rs.next() ? (UUID) rs.getObject("latest_job_id") : null,
                workspaceId, checksum, fileSize
        );
    }

    @Override
    public void upsertDedup(UUID workspaceId, String checksum, long fileSize, UUID latestJobId) {
        jdbcTemplate.update(
                "insert into import_dedup (id, workspace_id, checksum, file_size, latest_job_id, created_at, updated_at) " +
                        "values (?, ?, ?, ?, ?, now(), now()) " +
                        "on conflict (workspace_id, checksum, file_size) do update set latest_job_id = excluded.latest_job_id, updated_at = now()",
                UUID.randomUUID(), workspaceId, checksum, fileSize, latestJobId
        );
    }
}
