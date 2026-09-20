package com.openSupports.demo.infra.exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, Long id) {
        super("RESOURCE_NOT_FOUND", resource + " not found with id: " + id);
    }
}
