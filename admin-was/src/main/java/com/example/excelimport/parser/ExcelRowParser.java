package com.example.excelimport.parser;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExcelRowParser implements RowParser {

    @Override
    public boolean supports(String extension) {
        return "xlsx".equalsIgnoreCase(extension);
    }

    @Override
    public List<RowData> parse(Path filePath) throws IOException {
        List<RowData> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream in = Files.newInputStream(filePath); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                int lastCellNum = Math.max(0, row.getLastCellNum());
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < lastCellNum; i++) {
                    String value = formatter.formatCellValue(row.getCell(i));
                    values.put("col_" + (i + 1), value);
                }
                rows.add(new RowData(row.getRowNum() + 1, values));
            }
        }
        return rows;
    }
}
