package com.sabbpe.exception;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message, 401);
    }
}
