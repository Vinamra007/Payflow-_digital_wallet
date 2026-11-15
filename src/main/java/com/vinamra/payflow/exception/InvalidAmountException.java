package com.vinamra.payflow.exception;

public class InvalidAmountException extends InvalidRequestException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
