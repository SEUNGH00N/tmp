package com.example.excelimport.repository;

import com.example.excelimport.entity.ExcelRowData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExcelRowDataRepository extends JpaRepository<ExcelRowData, UUID> {
}
