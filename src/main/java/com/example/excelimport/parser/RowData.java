package com.example.excelimport.parser;

import java.util.Map;

public record RowData(int rowIndex, Map<String, String> values) {
}
