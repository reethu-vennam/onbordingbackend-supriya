package com.sabbpe.exception;

public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message, 400);
    }

    public BadRequestException(String message, Object details) {
        super("BAD_REQUEST", message, 400, details);
    }
}
