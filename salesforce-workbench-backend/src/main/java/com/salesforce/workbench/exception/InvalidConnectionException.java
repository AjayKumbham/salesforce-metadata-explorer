package com.salesforce.workbench.exception;

public class InvalidConnectionException extends RuntimeException {
    public InvalidConnectionException(String message) {
        super(message);
    }
}
