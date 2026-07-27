package com.sabbpe.exception;

public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resource, String field, String value) {
        super("NOT_FOUND", resource + " not found with " + field + ": " + value, 404);
    }
}
