package com.vinamra.payflow.repository;

import com.vinamra.payflow.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> byUsername = new ConcurrentHashMap<>();
    private final Map<String, User> byId = new ConcurrentHashMap<>();

    @Override
    public boolean saveIfAbsent(User user) {
        String key = user.username().toLowerCase(Locale.ROOT);
        if (byUsername.putIfAbsent(key, user) != null) {
            return false;
        }
        byId.put(user.id(), user);
        return true;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(byUsername.get(username.toLowerCase(Locale.ROOT)));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
