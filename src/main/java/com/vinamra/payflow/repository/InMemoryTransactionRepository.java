package com.vinamra.payflow.repository;

import com.vinamra.payflow.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryTransactionRepository implements TransactionRepository {
    private final List<Transaction> all = new CopyOnWriteArrayList<>();
    private final Map<String, Transaction> byIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        all.add(transaction);
        if (transaction.idempotencyKey() != null) {
            byIdempotencyKey.put(transaction.idempotencyKey(), transaction);
        }
        return transaction;
    }

    @Override
    public List<Transaction> findByAccountId(String accountId) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : all) {
            if (accountId.equals(t.fromAccountId()) || accountId.equals(t.toAccountId())) {
                result.add(t);
            }
        }
        return result;
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(all);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String key) {
        return Optional.ofNullable(byIdempotencyKey.get(key));
    }
}
