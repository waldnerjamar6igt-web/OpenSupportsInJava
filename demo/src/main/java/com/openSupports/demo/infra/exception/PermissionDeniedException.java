package com.openSupports.demo.infra.exception;

public class PermissionDeniedException extends AppException {
    public PermissionDeniedException(String message) {
        super("PERMISSION_DENIED", message);
    }
}
