package com.powerpuff.backend.commerce;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    public final HttpStatus status;
    public final String code;
    public BusinessException(HttpStatus status,String code,String message) {super(message);this.status=status;this.code=code;}
}
