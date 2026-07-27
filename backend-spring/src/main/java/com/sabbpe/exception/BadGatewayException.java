package com.sabbpe.exception;

public class BadGatewayException extends AppException {

    public BadGatewayException(String message) {
        super("BAD_GATEWAY", message, 502);
    }
}
