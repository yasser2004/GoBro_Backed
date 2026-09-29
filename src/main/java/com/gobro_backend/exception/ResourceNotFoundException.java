package com.gobro_backend.exception;



public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super("%s introuvable avec %s = '%s'".formatted(resourceName, fieldName, fieldValue));
    }
}