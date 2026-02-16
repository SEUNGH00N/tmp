package com.example.excelimport.validation;

import com.example.excelimport.parser.RowData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RowValidator {

    public List<ValidationError> validate(RowData rowData) {
        List<ValidationError> errors = new ArrayList<>();
        String col1 = rowData.values().getOrDefault("col_1", "");
        if (col1 == null || col1.trim().isEmpty()) {
            errors.add(new ValidationError(rowData.rowIndex(), "col_1", "IMP-VAL-001", "col_1 is required"));
        }
        for (Map.Entry<String, String> entry : rowData.values().entrySet()) {
            String value = entry.getValue();
            if (value != null && value.length() > 255) {
                errors.add(new ValidationError(rowData.rowIndex(), entry.getKey(), "IMP-VAL-002", "cell length must be <= 255"));
            }
        }
        return errors;
    }
}
