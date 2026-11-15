package com.vinamra.payflow.repository;

import com.vinamra.payflow.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    /** Atomically stores the user; returns false if the username already exists (case-insensitive). */
    boolean saveIfAbsent(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);

    List<User> findAll();
}
