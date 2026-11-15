package com.vinamra.payflow.exception;

public class UsernameTakenException extends PayFlowException {
    public UsernameTakenException() {
        super("USERNAME_TAKEN", "That username is already taken");
    }
}
