package com.openSupports.demo.infra.exception;

public class BusinessRuleViolationException extends AppException {
    public BusinessRuleViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
