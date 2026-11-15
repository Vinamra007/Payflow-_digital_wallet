package com.vinamra.payflow.exception;

public class IdempotencyConflictException extends PayFlowException {
    public IdempotencyConflictException(String key) {
        super("IDEMPOTENCY_CONFLICT",
                "Idempotency key '" + key + "' was already used for a different transfer");
    }
}
