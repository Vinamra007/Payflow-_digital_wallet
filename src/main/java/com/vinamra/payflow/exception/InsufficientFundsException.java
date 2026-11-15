package com.vinamra.payflow.exception;

public class InsufficientFundsException extends PayFlowException {
    public InsufficientFundsException(String accountId) {
        super("INSUFFICIENT_FUNDS", "Insufficient funds in account " + accountId);
    }
}
