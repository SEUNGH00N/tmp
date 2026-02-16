package com.example.excelimport.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvRowParser implements RowParser {

    @Override
    public boolean supports(String extension) {
        return "csv".equalsIgnoreCase(extension);
    }

    @Override
    public List<RowData> parse(Path filePath) throws IOException {
        List<RowData> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);
            int idx = 1;
            for (CSVRecord record : records) {
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < record.size(); i++) {
                    values.put("col_" + (i + 1), record.get(i));
                }
                rows.add(new RowData(idx, values));
                idx++;
            }
        }
        return rows;
    }
}
