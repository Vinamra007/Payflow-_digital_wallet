package com.vinamra.payflow.model;

import java.time.Instant;

/** An immutable record of money movement. */
public record Transaction(
        String id,
        Type type,
        String fromAccountId,   // null for deposits
        String toAccountId,     // null for withdrawals
        long amountMinor,
        Instant createdAt,
        String idempotencyKey   // null when not supplied
) {
    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER }
}
