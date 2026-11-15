package com.vinamra.payflow.exception;

/** Not logged in, session expired, or wrong credentials. */
public class UnauthorizedException extends PayFlowException {
    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
