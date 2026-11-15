package com.vinamra.payflow.exception;

public class AccountNotFoundException extends PayFlowException {
    public AccountNotFoundException(String accountId) {
        super("ACCOUNT_NOT_FOUND", "Account not found: " + accountId);
    }
}
