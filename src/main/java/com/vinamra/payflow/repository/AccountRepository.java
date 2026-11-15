package com.vinamra.payflow.repository;

import com.vinamra.payflow.model.Account;

import java.util.List;
import java.util.Optional;

/** Storage abstraction. A MySQL/JDBC implementation can replace the in-memory one without touching the service. */
public interface AccountRepository {
    Account create(String ownerUserId, String ownerName, String label, long initialBalanceMinor);

    Optional<Account> findById(String id);

    List<Account> findByUserId(String ownerUserId);

    List<Account> findAll();
}
