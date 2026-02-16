package com.example.excelimport.userwas.exception;

public class UserWasException extends RuntimeException {
    private final int status;

    public UserWasException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
