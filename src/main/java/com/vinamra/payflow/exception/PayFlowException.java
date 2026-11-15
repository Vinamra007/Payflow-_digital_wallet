package com.vinamra.payflow.exception;

/** Base class for all business errors. Each error carries a stable machine-readable code. */
public class PayFlowException extends RuntimeException {
    private final String code;

    public PayFlowException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
