package com.sabbpe.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final String code;
    private final int statusCode;
    private final Object details;

    public AppException(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
        this.details = null;
    }

    public AppException(String code, String message, int statusCode, Object details) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
        this.details = details;
    }
}
