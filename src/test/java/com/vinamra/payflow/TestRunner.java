package com.vinamra.payflow;

import com.vinamra.payflow.api.ApiServer;
import com.vinamra.payflow.exception.AccountNotFoundException;
import com.vinamra.payflow.exception.ForbiddenException;
import com.vinamra.payflow.exception.InvalidRequestException;
import com.vinamra.payflow.exception.TooManyAttemptsException;
import com.vinamra.payflow.exception.UnauthorizedException;
import com.vinamra.payflow.exception.UsernameTakenException;
import com.vinamra.payflow.exception.IdempotencyConflictException;
import com.vinamra.payflow.exception.InsufficientFundsException;
import com.vinamra.payflow.exception.InvalidAmountException;
import com.vinamra.payflow.model.Account;
import com.vinamra.payflow.model.Transaction;
import com.vinamra.payflow.model.Role;
import com.vinamra.payflow.model.User;
import com.vinamra.payflow.repository.InMemoryAccountRepository;
import com.vinamra.payflow.repository.InMemoryTransactionRepository;
import com.vinamra.payflow.repository.InMemoryUserRepository;
import com.vinamra.payflow.repository.UserRepository;
import com.vinamra.payflow.service.AdminService;
import com.vinamra.payflow.service.AuthResult;
import com.vinamra.payflow.service.AuthService;
import com.vinamra.payflow.service.WalletService;
import com.vinamra.payflow.util.Json;
import com.vinamra.payflow.util.Money;
import com.vinamra.payflow.util.PasswordHasher;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tiny dependency-free test runner so the project builds with just a JDK.
 * (Migrating these to JUnit 5 is a good next step.)
 */
public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    interface Test {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        run("Money converts decimals to minor units", TestRunner::moneyConversion);
        run("Money rejects more than 2 decimal places", TestRunner::moneyRejectsBadScale);
        run("Json parses flat objects and escapes output", TestRunner::jsonRoundTrip);
        run("Json rejects malformed input", TestRunner::jsonRejectsMalformed);
        run("Deposit and withdraw update balance", TestRunner::depositWithdraw);
        run("Withdraw beyond balance is rejected", TestRunner::insufficientFunds);
        run("Non-positive amounts are rejected", TestRunner::invalidAmounts);
        run("Unknown account raises not-found", TestRunner::unknownAccount);
        run("Transfer moves money between accounts", TestRunner::transferMovesMoney);
        run("Failed transfer changes nothing", TestRunner::failedTransferAtomic);
        run("Idempotent transfer is applied once", TestRunner::idempotentTransfer);
        run("Reused idempotency key with different data conflicts", TestRunner::idempotencyConflict);
        run("Opposite-direction transfers do not deadlock", TestRunner::noDeadlock);
        run("Concurrent transfers conserve total money", TestRunner::concurrentConservation);
        run("Password hashing is salted and verifiable", TestRunner::passwordHashing);
        run("Register then authenticate with the session token", TestRunner::registerAndLogin);
        run("Duplicate usernames are rejected", TestRunner::duplicateUsername);
        run("Weak or invalid sign-up input is rejected", TestRunner::weakInputRejected);
        run("Wrong password is rejected with a generic error", TestRunner::wrongPassword);
        run("Five failed logins lock the username out", TestRunner::lockout);
        run("Sessions expire and logout invalidates the token", TestRunner::sessionExpiryAndLogout);
        run("Users can only access their own accounts", TestRunner::ownershipEnforced);
        run("Account limit per user is enforced", TestRunner::accountLimit);
        run("Bank owner role cannot be obtained by registering", TestRunner::adminRole);
        run("Owner overview totals and who-did-what history", TestRunner::adminOverview);
        run("Owner snapshots stay consistent while money moves", TestRunner::adminSnapshotConsistency);
        run("REST API end-to-end flow with login and ownership", TestRunner::apiFlow);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void run(String name, Test test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + "  ->  " + t);
        }
    }

    private static WalletService newService() {
        return new WalletService(new InMemoryAccountRepository(), new InMemoryTransactionRepository());
    }

    /** Creates an account owned by a throwaway user named after the account owner. */
    private static Account acct(WalletService s, String name, long amountMinor) {
        return s.createAccount("u-" + name, name, "Wallet", amountMinor);
    }

    private static long rupees(String amount) {
        return Money.toMinor(new BigDecimal(amount));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void checkEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but was " + actual);
    }

    private static <T extends Throwable> void expectThrows(Class<T> type, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }

    // ---------- unit tests ----------
    static void moneyConversion() {
        checkEquals(25050L, Money.toMinor(new BigDecimal("250.50")));
        checkEquals(1000L, Money.toMinor(new BigDecimal("10")));
        checkEquals("250.50", Money.toDecimal(25050).toPlainString());
    }

    static void moneyRejectsBadScale() {
        expectThrows(InvalidAmountException.class, () -> Money.toMinor(new BigDecimal("1.234")));
    }

    static void jsonRoundTrip() {
        Map<String, String> m = Json.parseObject("{ \"owner\": \"Asha \\\"A\\\"\", \"amount\": 12.50, \"x\": null }");
        checkEquals("Asha \"A\"", m.get("owner"));
        checkEquals("12.50", m.get("amount"));
        check(m.get("x") == null, "null value should parse to null");
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("a", "q\"");
        out.put("b", 1);
        checkEquals("{\"a\":\"q\\\"\",\"b\":1}", Json.toJson(out));
    }

    static void jsonRejectsMalformed() {
        expectThrows(IllegalArgumentException.class, () -> Json.parseObject("{\"a\":"));
        expectThrows(IllegalArgumentException.class, () -> Json.parseObject("not json"));
        expectThrows(IllegalArgumentException.class, () -> Json.parseObject("{\"a\":{\"b\":1}}"));
    }

    static void depositWithdraw() {
        WalletService s = newService();
        Account a = acct(s, "Asha", rupees("100"));
        s.deposit(a.getId(), rupees("50.25"));
        s.withdraw(a.getId(), rupees("20"));
        checkEquals(rupees("130.25"), a.getBalanceMinor());
        checkEquals(3, s.history(a.getId()).size()); // initial deposit, deposit, withdrawal
    }

    static void insufficientFunds() {
        WalletService s = newService();
        Account a = acct(s, "Asha", rupees("10"));
        expectThrows(InsufficientFundsException.class, () -> s.withdraw(a.getId(), rupees("10.01")));
        checkEquals(rupees("10"), a.getBalanceMinor());
    }

    static void invalidAmounts() {
        WalletService s = newService();
        Account a = acct(s, "Asha", rupees("10"));
        expectThrows(InvalidAmountException.class, () -> s.deposit(a.getId(), 0));
        expectThrows(InvalidAmountException.class, () -> s.withdraw(a.getId(), -5));
    }

    static void unknownAccount() {
        WalletService s = newService();
        expectThrows(AccountNotFoundException.class, () -> s.getAccount("ACC-0"));
        expectThrows(AccountNotFoundException.class, () -> s.deposit("ACC-0", 100));
    }

    static void transferMovesMoney() {
        WalletService s = newService();
        Account a = acct(s, "Asha", rupees("500"));
        Account b = acct(s, "Ravi", rupees("0"));
        s.transfer(a.getId(), b.getId(), rupees("125.50"), null);
        checkEquals(rupees("374.50"), a.getBalanceMinor());
        checkEquals(rupees("125.50"), b.getBalanceMinor());
    }

    static void failedTransferAtomic() {
        WalletService s = newService();
        Account a = acct(s, "Asha", rupees("50"));
        Account b = acct(s, "Ravi", rupees("10"));
        expectThrows(InsufficientFundsException.class, () -> s.transfer(a.getId(), b.getId(), rupees("80"), null));
        checkEquals(rupees("50"), a.getBalanceMinor());
        checkEquals(rupees("10"), b.getBalanceMinor());
    }

    static void idempotentTransfer() {
        WalletService s = newService();
        Account a = acct(s, "Asha", rupees("100"));
        Account b = acct(s, "Ravi", rupees("0"));
        Transaction first = s.transfer(a.getId(), b.getId(), rupees("40"), "req-1");
        Transaction second = s.transfer(a.getId(), b.getId(), rupees("40"), "req-1");
        checkEquals(first.id(), second.id());
        checkEquals(rupees("60"), a.getBalanceMinor());
        checkEquals(rupees("40"), b.getBalanceMinor());
    }

    static void idempotencyConflict() {
        WalletService s = newService();
        Account a = acct(s, "Asha", rupees("100"));
        Account b = acct(s, "Ravi", rupees("0"));
        s.transfer(a.getId(), b.getId(), rupees("40"), "req-1");
        expectThrows(IdempotencyConflictException.class,
                () -> s.transfer(a.getId(), b.getId(), rupees("41"), "req-1"));
    }

    // ---------- concurrency tests ----------
    static void noDeadlock() throws Exception {
        WalletService s = newService();
        Account a = acct(s, "A", rupees("100000"));
        Account b = acct(s, "B", rupees("100000"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> {
            for (int i = 0; i < 20000; i++) s.transfer(a.getId(), b.getId(), 100, null);
        });
        pool.submit(() -> {
            for (int i = 0; i < 20000; i++) s.transfer(b.getId(), a.getId(), 100, null);
        });
        pool.shutdown();
        check(pool.awaitTermination(30, TimeUnit.SECONDS), "transfers did not finish (possible deadlock)");
        checkEquals(rupees("200000"), a.getBalanceMinor() + b.getBalanceMinor());
    }

    static void concurrentConservation() throws Exception {
        WalletService s = newService();
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < 5; i++) accounts.add(acct(s, "U" + i, rupees("10000")));
        long total = rupees("50000");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int t = 0; t < 8; t++) {
            pool.submit(() -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 3000; i++) {
                    int from = r.nextInt(accounts.size());
                    int to = (from + 1 + r.nextInt(accounts.size() - 1)) % accounts.size();
                    try {
                        s.transfer(accounts.get(from).getId(), accounts.get(to).getId(),
                                100 + r.nextInt(5000), null);
                    } catch (InsufficientFundsException ignored) {
                        // expected sometimes; the balance must simply never go negative
                    }
                }
            });
        }
        pool.shutdown();
        check(pool.awaitTermination(60, TimeUnit.SECONDS), "workers did not finish");

        long sum = 0;
        for (Account a : accounts) {
            check(a.getBalanceMinor() >= 0, "negative balance found");
            sum += a.getBalanceMinor();
        }
        checkEquals(total, sum);
    }

    // ---------- auth and ownership tests ----------
    private static AuthService newAuth() {
        return new AuthService(new InMemoryUserRepository());
    }

    static void passwordHashing() {
        String h = PasswordHasher.hash("secret123");
        check(PasswordHasher.verify("secret123", h), "correct password should verify");
        check(!PasswordHasher.verify("secret124", h), "wrong password should not verify");
        check(!h.equals(PasswordHasher.hash("secret123")), "two hashes of one password must differ (random salt)");
        check(!h.contains("secret123"), "hash must not contain the password");
    }

    static void registerAndLogin() {
        AuthService auth = newAuth();
        AuthResult r = auth.register("asha_k", "password1", "Asha K");
        checkEquals("asha_k", auth.authenticate(r.token()).username());
        AuthResult l = auth.login("ASHA_K", "password1"); // usernames are case-insensitive
        checkEquals(r.user().id(), auth.authenticate(l.token()).id());
        check(!r.token().equals(l.token()), "each login gets a fresh token");
    }

    static void duplicateUsername() {
        AuthService auth = newAuth();
        auth.register("asha", "password1", "Asha");
        expectThrows(UsernameTakenException.class, () -> auth.register("ASHA", "password2", "Other"));
    }

    static void weakInputRejected() {
        AuthService auth = newAuth();
        expectThrows(InvalidRequestException.class, () -> auth.register("ab", "password1", "X"));
        expectThrows(InvalidRequestException.class, () -> auth.register("bad name!", "password1", "X"));
        expectThrows(InvalidRequestException.class, () -> auth.register("valid_name", "short", "X"));
    }

    static void wrongPassword() {
        AuthService auth = newAuth();
        auth.register("asha", "password1", "Asha");
        UnauthorizedException wrong = null, unknown = null;
        try {
            auth.login("asha", "nope-nope");
        } catch (UnauthorizedException e) {
            wrong = e;
        }
        try {
            auth.login("nobody", "nope-nope");
        } catch (UnauthorizedException e) {
            unknown = e;
        }
        check(wrong != null && unknown != null, "both attempts must be rejected");
        checkEquals(wrong.getMessage(), unknown.getMessage()); // does not reveal which usernames exist
    }

    static void lockout() {
        AuthService auth = newAuth();
        auth.register("asha", "password1", "Asha");
        for (int i = 0; i < 5; i++) {
            expectThrows(UnauthorizedException.class, () -> auth.login("asha", "wrong-pass"));
        }
        expectThrows(TooManyAttemptsException.class, () -> auth.login("asha", "password1")); // even the right one
    }

    static void sessionExpiryAndLogout() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T10:00:00Z"));
        AuthService auth = new AuthService(new InMemoryUserRepository(), now::get);
        String token = auth.register("asha", "password1", "Asha").token();
        auth.authenticate(token); // valid now
        now.set(now.get().plus(Duration.ofHours(3)));
        expectThrows(UnauthorizedException.class, () -> auth.authenticate(token));

        String token2 = auth.login("asha", "password1").token();
        auth.authenticate(token2);
        auth.logout(token2);
        expectThrows(UnauthorizedException.class, () -> auth.authenticate(token2));
    }

    static void ownershipEnforced() {
        WalletService s = newService();
        Account a = s.createAccount("u1", "Asha", "Main", rupees("100"));
        checkEquals(a.getId(), s.requireOwner("u1", a.getId()).getId());
        expectThrows(ForbiddenException.class, () -> s.requireOwner("u2", a.getId()));
        s.createAccount("u2", "Ravi", "Main", 0);
        checkEquals(1, s.listAccounts("u1").size());
        checkEquals(1, s.listAccounts("u2").size());
    }

    static void accountLimit() {
        WalletService s = newService();
        for (int i = 0; i < WalletService.MAX_ACCOUNTS_PER_USER; i++) {
            s.createAccount("u1", "Asha", "A" + i, 0);
        }
        expectThrows(InvalidRequestException.class, () -> s.createAccount("u1", "Asha", "extra", 0));
    }

    // ---------- bank owner tests ----------
    static void adminRole() {
        AuthService auth = newAuth();
        AuthResult customer = auth.register("asha", "password1", "Asha");
        checkEquals(Role.CUSTOMER, customer.user().role());
        User owner = auth.createAdmin("owner", "ownerpass1", "Bank Owner");
        checkEquals(Role.ADMIN, owner.role());
        checkEquals(Role.ADMIN, auth.authenticate(auth.login("owner", "ownerpass1").token()).role());
        expectThrows(UsernameTakenException.class, () -> auth.register("owner", "password1", "Fake Owner"));
    }

    static void adminOverview() {
        WalletService w = newService();
        UserRepository ur = new InMemoryUserRepository();
        AuthService au = new AuthService(ur);
        AdminService ad = new AdminService(w, ur);
        au.createAdmin("owner", "ownerpass1", "Bank Owner");
        User asha = au.register("asha", "password1", "Asha Kumar").user();
        User ravi = au.register("ravi", "password1", "Ravi Rao").user();
        Account a = w.createAccount(asha.id(), "Asha Kumar", "Main", rupees("500"));
        Account b = w.createAccount(ravi.id(), "Ravi Rao", "Main", rupees("100"));
        w.deposit(a.getId(), rupees("50"));
        w.withdraw(b.getId(), rupees("30"));
        w.transfer(a.getId(), b.getId(), rupees("200"), null);

        AdminService.Overview o = ad.overview();
        checkEquals(2, o.customers()); // the owner is not counted as a customer
        checkEquals(2, o.accounts());
        checkEquals(5, o.transactions());
        checkEquals(rupees("620"), o.totalBalanceMinor());
        checkEquals(rupees("650"), o.depositedMinor());
        checkEquals(rupees("30"), o.withdrawnMinor());
        checkEquals(rupees("200"), o.transferredMinor());
        check(o.ledgerBalanced(), "ledger should balance");

        List<AdminService.TransactionView> txns = ad.transactions(10);
        checkEquals(Transaction.Type.TRANSFER, txns.get(0).transaction().type()); // newest first
        checkEquals("Asha Kumar", txns.get(0).fromOwner());
        checkEquals("ravi", txns.get(0).toUsername());
        checkEquals(1, ad.transactions(1).size());
        checkEquals(2, ad.customers().size());
        checkEquals(1, ad.customers().get(0).accounts());
    }

    static void adminSnapshotConsistency() throws Exception {
        WalletService w = newService();
        UserRepository ur = new InMemoryUserRepository();
        AdminService ad = new AdminService(w, ur);
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < 5; i++) accounts.add(w.createAccount("u" + i, "U" + i, "Main", rupees("10000")));

        AtomicBoolean broken = new AtomicBoolean(false);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        for (int t = 0; t < 6; t++) {
            final int worker = t;
            pool.submit(() -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 2000; i++) {
                    Account x = accounts.get(r.nextInt(accounts.size()));
                    Account y = accounts.get((accounts.indexOf(x) + 1 + r.nextInt(accounts.size() - 1)) % accounts.size());
                    try {
                        switch (r.nextInt(4)) {
                            case 0 -> w.deposit(x.getId(), 100 + r.nextInt(5000));
                            case 1 -> w.withdraw(x.getId(), 100 + r.nextInt(5000));
                            case 2 -> w.createAccount("w" + worker + "-" + i, "W", "New", 1000 + r.nextInt(100));
                            default -> w.transfer(x.getId(), y.getId(), 100 + r.nextInt(5000), null);
                        }
                    } catch (InsufficientFundsException ignored) {
                        // expected sometimes
                    }
                }
            });
        }
        pool.shutdown();
        int snapshots = 0;
        do {
            if (!ad.overview().ledgerBalanced()) broken.set(true);
            snapshots++;
        } while (!pool.isTerminated());
        check(pool.awaitTermination(60, TimeUnit.SECONDS), "workers did not finish");
        check(!broken.get(), "a snapshot saw money created or lost while transfers were running");
        check(snapshots > 0 && ad.overview().ledgerBalanced(), "final ledger must balance");
    }

    // ---------- API integration test ----------
    static void apiFlow() throws Exception {
        WalletService wallet = newService();
        UserRepository userRepo = new InMemoryUserRepository();
        AuthService authService = new AuthService(userRepo);
        authService.createAdmin("owner", "ownerpass1", "Bank Owner");
        ApiServer server = new ApiServer(wallet, authService, new AdminService(wallet, userRepo), 0);
        server.start();
        try {
            HttpClient c = HttpClient.newHttpClient();
            String base = "http://localhost:" + server.getPort();

            checkEquals(401, send(c, "GET", base + "/accounts", null, null).statusCode());
            HttpResponse<String> page = send(c, "GET", base + "/", null, null);
            checkEquals(200, page.statusCode());
            check(page.body().contains("PayFlow"), "dashboard page not served");

            HttpResponse<String> regA = send(c, "POST", base + "/auth/register", null,
                    "{\"username\":\"asha\",\"password\":\"password1\",\"name\":\"Asha Kumar\"}");
            checkEquals(201, regA.statusCode());
            String tokenA = tokenOf(regA.body());
            HttpResponse<String> regB = send(c, "POST", base + "/auth/register", null,
                    "{\"username\":\"ravi\",\"password\":\"password2\",\"name\":\"Ravi Rao\"}");
            String tokenB = tokenOf(regB.body());

            checkEquals(409, send(c, "POST", base + "/auth/register", null,
                    "{\"username\":\"asha\",\"password\":\"password9\"}").statusCode());
            checkEquals(401, send(c, "POST", base + "/auth/login", null,
                    "{\"username\":\"asha\",\"password\":\"wrong-pass\"}").statusCode());
            HttpResponse<String> login = send(c, "POST", base + "/auth/login", null,
                    "{\"username\":\"asha\",\"password\":\"password1\"}");
            checkEquals(200, login.statusCode());
            check(send(c, "GET", base + "/auth/me", tokenOf(login.body()), null).body().contains("Asha Kumar"), "me");

            HttpResponse<String> accA = send(c, "POST", base + "/accounts", tokenA,
                    "{\"label\":\"Main\",\"initialBalance\":500}");
            checkEquals(201, accA.statusCode());
            check(accA.body().contains("\"id\":\"ACC-1001\""), accA.body());
            check(accA.body().contains("\"owner\":\"Asha Kumar\""), accA.body());
            checkEquals(201, send(c, "POST", base + "/accounts", tokenB, "{\"label\":\"Wallet\"}").statusCode());

            // ownership: Ravi must not see or touch Asha's account
            checkEquals(403, send(c, "GET", base + "/accounts/ACC-1001", tokenB, null).statusCode());
            checkEquals(403, send(c, "GET", base + "/accounts/ACC-1001/transactions", tokenB, null).statusCode());
            checkEquals(403, send(c, "POST", base + "/accounts/ACC-1001/deposit", tokenB, "{\"amount\":10}").statusCode());
            checkEquals(403, send(c, "POST", base + "/accounts/ACC-1001/withdraw", tokenB, "{\"amount\":10}").statusCode());
            checkEquals(403, send(c, "POST", base + "/transfers", tokenB,
                    "{\"from\":\"ACC-1001\",\"to\":\"ACC-1002\",\"amount\":5}").statusCode());

            HttpResponse<String> listA = send(c, "GET", base + "/accounts", tokenA, null);
            check(listA.body().contains("ACC-1001") && !listA.body().contains("ACC-1002"), "list shows only own accounts");

            HttpResponse<String> lookup = send(c, "GET", base + "/lookup/ACC-1002", tokenA, null);
            checkEquals(200, lookup.statusCode());
            check(lookup.body().contains("Ravi Rao") && !lookup.body().contains("balance"), lookup.body());

            // transfers to someone else, with an idempotent retry
            String transfer = "{\"from\":\"ACC-1001\",\"to\":\"ACC-1002\",\"amount\":125.50,\"idempotencyKey\":\"k1\"}";
            HttpResponse<String> t1 = send(c, "POST", base + "/transfers", tokenA, transfer);
            checkEquals(201, t1.statusCode());
            check(t1.body().contains("\"idempotencyKey\":\"k1\""), t1.body());
            checkEquals(t1.body(), send(c, "POST", base + "/transfers", tokenA, transfer).body());
            check(send(c, "GET", base + "/accounts/ACC-1001", tokenA, null).body().contains("\"balance\":374.50"), "balance");
            check(send(c, "GET", base + "/accounts/ACC-1002/transactions", tokenB, null).body().contains("TRANSFER"), "history");

            // other errors
            checkEquals(422, send(c, "POST", base + "/accounts/ACC-1002/withdraw", tokenB, "{\"amount\":9999}").statusCode());
            checkEquals(404, send(c, "GET", base + "/lookup/ACC-9999", tokenA, null).statusCode());
            checkEquals(400, send(c, "POST", base + "/accounts/ACC-1001/deposit", tokenA, "{\"amount\":-5}").statusCode());
            checkEquals(400, send(c, "POST", base + "/accounts/ACC-1001/deposit", tokenA, "{bad json").statusCode());
            checkEquals(405, send(c, "GET", base + "/transfers", tokenA, null).statusCode());
            checkEquals(401, send(c, "GET", base + "/accounts", "not-a-real-token", null).statusCode());

            // bank owner console
            checkEquals(401, send(c, "GET", base + "/admin/overview", null, null).statusCode());
            checkEquals(403, send(c, "GET", base + "/admin/overview", tokenA, null).statusCode());
            checkEquals(403, send(c, "GET", base + "/admin/transactions", tokenB, null).statusCode());
            HttpResponse<String> ownerLogin = send(c, "POST", base + "/auth/login", null,
                    "{\"username\":\"owner\",\"password\":\"ownerpass1\"}");
            checkEquals(200, ownerLogin.statusCode());
            check(ownerLogin.body().contains("\"role\":\"ADMIN\""), ownerLogin.body());
            String tokenO = tokenOf(ownerLogin.body());
            HttpResponse<String> overview = send(c, "GET", base + "/admin/overview", tokenO, null);
            checkEquals(200, overview.statusCode());
            check(overview.body().contains("\"customers\":2") && overview.body().contains("\"accounts\":2")
                    && overview.body().contains("\"totalBalance\":500.00") && overview.body().contains("\"transactions\":2")
                    && overview.body().contains("\"ledgerBalanced\":true"), overview.body());
            HttpResponse<String> allTxns = send(c, "GET", base + "/admin/transactions", tokenO, null);
            check(allTxns.body().contains("Asha Kumar") && allTxns.body().contains("Ravi Rao")
                    && allTxns.body().contains("\"type\":\"TRANSFER\""), allTxns.body());
            check(send(c, "GET", base + "/admin/accounts", tokenO, null).body().contains("ACC-1002"), "admin accounts");
            check(send(c, "GET", base + "/admin/users", tokenO, null).body().contains("@") == false
                    && send(c, "GET", base + "/admin/users", tokenO, null).body().contains("ravi"), "admin users");
            checkEquals(403, send(c, "POST", base + "/accounts", tokenO, "{\"label\":\"x\"}").statusCode());
            checkEquals(404, send(c, "GET", base + "/admin/nothing", tokenO, null).statusCode());

            // logout kills the token
            checkEquals(200, send(c, "POST", base + "/auth/logout", tokenA, null).statusCode());
            checkEquals(401, send(c, "GET", base + "/accounts", tokenA, null).statusCode());
        } finally {
            server.stop();
        }
    }

    private static String tokenOf(String json) {
        Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(json);
        if (!m.find()) throw new AssertionError("no token in " + json);
        return m.group(1);
    }

    private static HttpResponse<String> send(HttpClient c, String method, String url, String token, String body)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url));
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (body != null) b.header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return c.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
