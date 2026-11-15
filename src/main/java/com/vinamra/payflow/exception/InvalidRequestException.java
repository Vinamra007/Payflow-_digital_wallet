package com.vinamra.payflow.exception;

public class InvalidRequestException extends PayFlowException {
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }
}
