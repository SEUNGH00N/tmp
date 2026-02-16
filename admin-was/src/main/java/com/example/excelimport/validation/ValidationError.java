package com.example.excelimport.validation;

public record ValidationError(int rowIndex, String column, String code, String message) {
}
