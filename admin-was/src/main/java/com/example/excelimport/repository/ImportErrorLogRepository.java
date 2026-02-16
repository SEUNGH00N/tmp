package com.example.excelimport.repository;

import com.example.excelimport.entity.ImportErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportErrorLogRepository extends JpaRepository<ImportErrorLog, UUID> {
    Page<ImportErrorLog> findByJobId(UUID jobId, Pageable pageable);
}
