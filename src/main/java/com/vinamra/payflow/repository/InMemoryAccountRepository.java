package com.vinamra.payflow.repository;

import com.vinamra.payflow.model.Account;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    @Override
    public Account create(String ownerUserId, String ownerName, String label, long initialBalanceMinor) {
        String id = "ACC-" + sequence.incrementAndGet();
        Account account = new Account(id, ownerUserId, ownerName, label, initialBalanceMinor);
        accounts.put(id, account);
        return account;
    }

    @Override
    public Optional<Account> findById(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(accounts.values());
    }

    @Override
    public List<Account> findByUserId(String ownerUserId) {
        List<Account> list = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.getOwnerUserId().equals(ownerUserId)) list.add(a);
        }
        list.sort(Comparator.comparing(Account::getCreatedAt).thenComparing(Account::getId));
        return list;
    }
}
