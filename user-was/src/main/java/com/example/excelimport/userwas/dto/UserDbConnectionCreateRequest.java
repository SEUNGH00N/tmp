package com.example.excelimport.userwas.dto;

public record UserDbConnectionCreateRequest(
        String name,
        String dbType,
        String host,
        Integer port,
        String dbName,
        String username,
        String password,
        String secretRef,
        String sslMode
) {
}
