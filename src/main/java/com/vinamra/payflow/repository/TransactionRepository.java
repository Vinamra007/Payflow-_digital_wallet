package com.vinamra.payflow.repository;

import com.vinamra.payflow.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findByAccountId(String accountId);

    Optional<Transaction> findByIdempotencyKey(String key);

    /** All transactions, oldest first. */
    List<Transaction> findAll();
}
