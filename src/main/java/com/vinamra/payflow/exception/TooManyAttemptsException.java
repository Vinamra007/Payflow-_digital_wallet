package com.vinamra.payflow.exception;

public class TooManyAttemptsException extends PayFlowException {
    public TooManyAttemptsException() {
        super("TOO_MANY_ATTEMPTS", "Too many failed logins. Please wait a minute and try again");
    }
}
