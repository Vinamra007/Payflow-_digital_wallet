package com.vinamra.payflow.service;

import com.vinamra.payflow.exception.InvalidRequestException;
import com.vinamra.payflow.exception.TooManyAttemptsException;
import com.vinamra.payflow.exception.UnauthorizedException;
import com.vinamra.payflow.exception.UsernameTakenException;
import com.vinamra.payflow.model.Role;
import com.vinamra.payflow.model.User;
import com.vinamra.payflow.repository.UserRepository;
import com.vinamra.payflow.util.PasswordHasher;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Registration, login and sessions.
 *  - passwords are stored only as salted PBKDF2 hashes;
 *  - a login returns a random 256-bit session token that expires after two hours;
 *  - five wrong passwords lock that username out for one minute (slows down guessing);
 *  - the same generic error is returned for "unknown user" and "wrong password".
 */
public class AuthService {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Duration SESSION_TTL = Duration.ofHours(2);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private record Session(String userId, Instant expiresAt) {
    }

    private record Attempts(int count, Instant lockedUntil) {
    }

    private final UserRepository users;
    private final Supplier<Instant> clock;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final AtomicLong userSequence = new AtomicLong(1000);

    public AuthService(UserRepository users) {
        this(users, Instant::now);
    }

    /** The clock can be replaced in tests to check session expiry without waiting. */
    public AuthService(UserRepository users, Supplier<Instant> clock) {
        this.users = users;
        this.clock = clock;
    }

    public AuthResult register(String username, String password, String displayName) {
        User user = buildUser(username, password, displayName, Role.CUSTOMER);
        if (!users.saveIfAbsent(user)) {
            throw new UsernameTakenException();
        }
        return newSession(user);
    }

    /**
     * Creates the bank owner. Called once at server start-up and never exposed through the API,
     * so nobody can obtain the ADMIN role by registering.
     */
    public User createAdmin(String username, String password, String displayName) {
        User admin = buildUser(username, password, displayName, Role.ADMIN);
        if (!users.saveIfAbsent(admin)) {
            throw new UsernameTakenException();
        }
        return admin;
    }

    private User buildUser(String username, String password, String displayName, Role role) {
        if (username == null || !USERNAME.matcher(username.trim()).matches()) {
            throw new InvalidRequestException("Username must be 3-20 characters: letters, digits or underscore");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new InvalidRequestException("Password must be 8 to 72 characters");
        }
        String cleanUsername = username.trim();
        String name = displayName == null || displayName.isBlank() ? cleanUsername : displayName.trim();
        if (name.length() > 40) {
            throw new InvalidRequestException("Name must be at most 40 characters");
        }
        return new User("USR-" + userSequence.incrementAndGet(), cleanUsername, name,
                PasswordHasher.hash(password), role);
    }

    public AuthResult login(String username, String password) {
        String key = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        checkNotLocked(key);
        if (password == null || password.isEmpty() || key.isEmpty()) {
            recordFailure(key);
            throw invalidCredentials();
        }
        User user = users.findByUsername(key).orElse(null);
        // Hash even for unknown users so timing does not reveal which usernames exist.
        boolean ok = PasswordHasher.verify(password, user != null ? user.passwordHash() : DUMMY_HASH) && user != null;
        if (!ok) {
            recordFailure(key);
            throw invalidCredentials();
        }
        attempts.remove(key);
        return newSession(user);
    }

    public User authenticate(String token) {
        Session session = token == null ? null : sessions.get(token);
        if (session == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Please log in");
        }
        if (clock.get().isAfter(session.expiresAt())) {
            sessions.remove(token);
            throw new UnauthorizedException("UNAUTHORIZED", "Your session has expired. Please log in again");
        }
        return users.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "Please log in"));
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    private static final String DUMMY_HASH = PasswordHasher.hash("dummy-password-for-timing");

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", "Invalid username or password");
    }

    private AuthResult newSession(User user) {
        Instant now = clock.get();
        sessions.values().removeIf(s -> now.isAfter(s.expiresAt()));
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        sessions.put(token, new Session(user.id(), now.plus(SESSION_TTL)));
        return new AuthResult(token, user);
    }

    private synchronized void checkNotLocked(String key) {
        Attempts a = attempts.get(key);
        if (a != null && a.lockedUntil() != null && clock.get().isBefore(a.lockedUntil())) {
            throw new TooManyAttemptsException();
        }
    }

    private synchronized void recordFailure(String key) {
        Attempts a = attempts.getOrDefault(key, new Attempts(0, null));
        int count = a.count() + 1;
        if (count >= MAX_FAILED_ATTEMPTS) {
            attempts.put(key, new Attempts(0, clock.get().plus(LOCKOUT)));
        } else {
            attempts.put(key, new Attempts(count, null));
        }
    }
}
