package com.vinamra.payflow.exception;

/** Logged in, but trying to touch something that belongs to another user. */
public class ForbiddenException extends PayFlowException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
