package com.example.excelimport.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface RowParser {
    boolean supports(String extension);
    List<RowData> parse(Path filePath) throws IOException;
}
