package com.powerpuff.backend.exception;

import org.springframework.http.HttpStatus;

public class EktException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public EktException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
