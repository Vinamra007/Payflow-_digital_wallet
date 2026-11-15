package com.vinamra.payflow.model;

import com.vinamra.payflow.exception.InsufficientFundsException;
import com.vinamra.payflow.exception.InvalidAmountException;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A wallet account owned by one user. The balance is protected by a per-account lock.
 * Callers that change the balance (credit/debit) must hold the lock; this is enforced
 * so that a missing lock shows up as a loud error instead of a silent race condition.
 */
public final class Account {
    private final String id;
    private final String ownerUserId;
    private final String owner;
    private final String label;
    private final Instant createdAt;
    private final ReentrantLock lock = new ReentrantLock();
    private long balanceMinor;

    public Account(String id, String ownerUserId, String owner, String label, long initialBalanceMinor) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.owner = owner;
        this.label = label;
        this.balanceMinor = initialBalanceMinor;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getOwner() {
        return owner;
    }

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public long getBalanceMinor() {
        lock.lock();
        try {
            return balanceMinor;
        } finally {
            lock.unlock();
        }
    }

    /** Caller must hold the lock. */
    public void credit(long amountMinor) {
        requireLocked();
        try {
            balanceMinor = Math.addExact(balanceMinor, amountMinor);
        } catch (ArithmeticException e) {
            throw new InvalidAmountException("balance limit exceeded");
        }
    }

    /** Caller must hold the lock. */
    public void debit(long amountMinor) {
        requireLocked();
        if (balanceMinor < amountMinor) {
            throw new InsufficientFundsException(id);
        }
        balanceMinor -= amountMinor;
    }

    private void requireLocked() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Account lock must be held to change the balance");
        }
    }
}
