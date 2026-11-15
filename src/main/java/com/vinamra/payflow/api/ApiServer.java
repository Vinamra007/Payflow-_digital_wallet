package com.vinamra.payflow.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.vinamra.payflow.exception.ForbiddenException;
import com.vinamra.payflow.exception.InvalidAmountException;
import com.vinamra.payflow.exception.InvalidRequestException;
import com.vinamra.payflow.exception.PayFlowException;
import com.vinamra.payflow.exception.UnauthorizedException;
import com.vinamra.payflow.model.Account;
import com.vinamra.payflow.model.Role;
import com.vinamra.payflow.model.Transaction;
import com.vinamra.payflow.model.User;
import com.vinamra.payflow.service.AdminService;
import com.vinamra.payflow.service.AdminService.AccountView;
import com.vinamra.payflow.service.AdminService.CustomerView;
import com.vinamra.payflow.service.AdminService.Overview;
import com.vinamra.payflow.service.AdminService.TransactionView;
import com.vinamra.payflow.service.AuthResult;
import com.vinamra.payflow.service.AuthService;
import com.vinamra.payflow.service.WalletService;
import com.vinamra.payflow.util.Json;
import com.vinamra.payflow.util.Money;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A small REST API built on the JDK's built-in HTTP server (no frameworks).
 * Everything except the dashboard page and /auth/register + /auth/login needs a
 * "Authorization: Bearer <token>" header, and users can only act on their own accounts.
 */
public class ApiServer {
    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final WalletService service;
    private final AuthService auth;
    private final AdminService admin;
    private final HttpServer server;

    public ApiServer(WalletService service, AuthService auth, AdminService admin, int port) throws IOException {
        this.service = service;
        this.auth = auth;
        this.admin = admin;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", this::handle);
        this.server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        ((ExecutorService) server.getExecutor()).shutdownNow();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange ex) throws IOException {
        try {
            route(ex);
        } catch (PayFlowException e) {
            send(ex, statusFor(e), error(e.getCode(), e.getMessage()));
        } catch (IllegalArgumentException e) { // malformed JSON, bad numbers
            send(ex, 400, error("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            send(ex, 500, error("INTERNAL_ERROR", "Unexpected server error"));
        }
    }

    private void route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] p = ex.getRequestURI().getPath().split("/"); // ["", "accounts", "ACC-1001", "deposit"]
        int n = p.length;

        if (n == 0 || (n == 2 && p[1].equals("index.html"))) {
            requireMethod(method, "GET");
            sendHtml(ex, Dashboard.HTML);
        } else if (n == 3 && p[1].equals("auth")) {
            routeAuth(ex, method, p[2]);
        } else {
            User user = authenticate(ex);
            routeApi(ex, method, p, n, user);
        }
    }

    private void routeAuth(HttpExchange ex, String method, String action) throws IOException {
        switch (action) {
            case "register" -> {
                requireMethod(method, "POST");
                Map<String, String> b = readBody(ex);
                AuthResult r = auth.register(b.get("username"), b.get("password"), b.get("name"));
                send(ex, 201, authJson(r));
            }
            case "login" -> {
                requireMethod(method, "POST");
                Map<String, String> b = readBody(ex);
                send(ex, 200, authJson(auth.login(b.get("username"), b.get("password"))));
            }
            case "logout" -> {
                requireMethod(method, "POST");
                auth.logout(requireToken(ex));
                send(ex, 200, "{\"ok\":true}");
            }
            case "me" -> {
                requireMethod(method, "GET");
                send(ex, 200, Json.toJson(userMap(authenticate(ex))));
            }
            default -> send(ex, 404, error("NOT_FOUND", "Route not found"));
        }
    }

    private void routeApi(HttpExchange ex, String method, String[] p, int n, User user) throws IOException {
        String uid = user.id();
        if (n == 2 && p[1].equals("accounts") && method.equals("GET")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Account a : service.listAccounts(uid)) list.add(accountMap(a));
            send(ex, 200, Json.toJson(list));
        } else if (n == 2 && p[1].equals("accounts")) {
            requireMethod(method, "POST");
            if (user.role() == Role.ADMIN) {
                throw new ForbiddenException("The bank owner account cannot hold wallets");
            }
            Map<String, String> body = readBody(ex);
            long initial = body.get("initialBalance") == null ? 0 : Money.toMinor(decimal(body.get("initialBalance")));
            Account a = service.createAccount(uid, user.displayName(), body.get("label"), initial);
            send(ex, 201, Json.toJson(accountMap(a)));
        } else if (n == 3 && p[1].equals("accounts")) {
            requireMethod(method, "GET");
            send(ex, 200, Json.toJson(accountMap(service.requireOwner(uid, p[2]))));
        } else if (n == 4 && p[1].equals("accounts") && p[3].equals("deposit")) {
            requireMethod(method, "POST");
            service.requireOwner(uid, p[2]);
            long amount = Money.toMinor(decimal(readBody(ex).get("amount")));
            send(ex, 201, transactionJson(service.deposit(p[2], amount)));
        } else if (n == 4 && p[1].equals("accounts") && p[3].equals("withdraw")) {
            requireMethod(method, "POST");
            service.requireOwner(uid, p[2]);
            long amount = Money.toMinor(decimal(readBody(ex).get("amount")));
            send(ex, 201, transactionJson(service.withdraw(p[2], amount)));
        } else if (n == 4 && p[1].equals("accounts") && p[3].equals("transactions")) {
            requireMethod(method, "GET");
            service.requireOwner(uid, p[2]);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Transaction t : service.history(p[2])) list.add(transactionMap(t));
            send(ex, 200, Json.toJson(list));
        } else if (n == 3 && p[1].equals("lookup")) {
            requireMethod(method, "GET");
            Account a = service.getAccount(p[2]); // only the public name is revealed
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("owner", a.getOwner());
            send(ex, 200, Json.toJson(m));
        } else if (n == 2 && p[1].equals("transfers")) {
            requireMethod(method, "POST");
            Map<String, String> body = readBody(ex);
            String from = body.get("from");
            if (from == null) throw new InvalidRequestException("from and to accounts are required");
            service.requireOwner(uid, from); // you can only send money out of your own accounts
            long amount = Money.toMinor(decimal(body.get("amount")));
            // Idempotency keys are scoped per user so one user can never replay another user's key.
            String key = body.get("idempotencyKey") == null ? null : uid + ":" + body.get("idempotencyKey");
            send(ex, 201, transactionJson(service.transfer(from, body.get("to"), amount, key)));
        } else if (n == 3 && p[1].equals("admin")) {
            requireMethod(method, "GET");
            if (user.role() != Role.ADMIN) {
                throw new ForbiddenException("Bank owner access only");
            }
            routeAdmin(ex, p[2]);
        } else {
            send(ex, 404, error("NOT_FOUND", "Route not found"));
        }
    }

    private void routeAdmin(HttpExchange ex, String what) throws IOException {
        switch (what) {
            case "overview" -> send(ex, 200, Json.toJson(overviewMap(admin.overview())));
            case "accounts" -> {
                List<Map<String, Object>> list = new ArrayList<>();
                for (AccountView a : admin.accounts()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.id());
                    m.put("label", a.label());
                    m.put("owner", a.owner());
                    m.put("username", a.username());
                    m.put("balance", Money.toDecimal(a.balanceMinor()));
                    m.put("createdAt", a.createdAt().toString());
                    list.add(m);
                }
                send(ex, 200, Json.toJson(list));
            }
            case "users" -> {
                List<Map<String, Object>> list = new ArrayList<>();
                for (CustomerView c : admin.customers()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", c.name());
                    m.put("username", c.username());
                    m.put("accounts", c.accounts());
                    m.put("balance", Money.toDecimal(c.balanceMinor()));
                    list.add(m);
                }
                send(ex, 200, Json.toJson(list));
            }
            case "transactions" -> {
                List<Map<String, Object>> list = new ArrayList<>();
                for (TransactionView v : admin.transactions(queryInt(ex, "limit", 500, 1000))) {
                    Transaction t = v.transaction();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.id());
                    m.put("type", t.type().name());
                    m.put("amount", Money.toDecimal(t.amountMinor()));
                    m.put("createdAt", t.createdAt().toString());
                    m.put("fromAccount", t.fromAccountId());
                    m.put("fromOwner", v.fromOwner());
                    m.put("fromUsername", v.fromUsername());
                    m.put("toAccount", t.toAccountId());
                    m.put("toOwner", v.toOwner());
                    m.put("toUsername", v.toUsername());
                    list.add(m);
                }
                send(ex, 200, Json.toJson(list));
            }
            default -> send(ex, 404, error("NOT_FOUND", "Route not found"));
        }
    }

    private Map<String, Object> overviewMap(Overview o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customers", o.customers());
        m.put("accounts", o.accounts());
        m.put("totalBalance", Money.toDecimal(o.totalBalanceMinor()));
        m.put("transactions", o.transactions());
        m.put("deposited", Money.toDecimal(o.depositedMinor()));
        m.put("withdrawn", Money.toDecimal(o.withdrawnMinor()));
        m.put("transferred", Money.toDecimal(o.transferredMinor()));
        m.put("depositCount", o.depositCount());
        m.put("withdrawalCount", o.withdrawalCount());
        m.put("transferCount", o.transferCount());
        m.put("expectedBalance", Money.toDecimal(o.expectedBalanceMinor()));
        m.put("ledgerBalanced", o.ledgerBalanced());
        return m;
    }

    private int queryInt(HttpExchange ex, String name, int fallback, int max) {
        String q = ex.getRequestURI().getQuery();
        if (q == null) return fallback;
        for (String part : q.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                try {
                    return Math.max(1, Math.min(max, Integer.parseInt(kv[1])));
                } catch (NumberFormatException e) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    // ---------- auth helpers ----------
    private String bearerToken(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7).trim();
    }

    private String requireToken(HttpExchange ex) {
        String token = bearerToken(ex);
        if (token == null) throw new UnauthorizedException("UNAUTHORIZED", "Please log in");
        return token;
    }

    private User authenticate(HttpExchange ex) {
        return auth.authenticate(requireToken(ex));
    }

    // ---------- misc helpers ----------
    private void requireMethod(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new MethodNotAllowed();
        }
    }

    private static final class MethodNotAllowed extends PayFlowException {
        MethodNotAllowed() {
            super("METHOD_NOT_ALLOWED", "HTTP method not allowed for this route");
        }
    }

    private int statusFor(PayFlowException e) {
        return switch (e.getCode()) {
            case "INVALID_REQUEST" -> 400;
            case "UNAUTHORIZED", "INVALID_CREDENTIALS" -> 401;
            case "FORBIDDEN" -> 403;
            case "ACCOUNT_NOT_FOUND" -> 404;
            case "METHOD_NOT_ALLOWED" -> 405;
            case "IDEMPOTENCY_CONFLICT", "USERNAME_TAKEN" -> 409;
            case "INSUFFICIENT_FUNDS" -> 422;
            case "TOO_MANY_ATTEMPTS" -> 429;
            default -> 500;
        };
    }

    private BigDecimal decimal(String raw) {
        if (raw == null) throw new InvalidAmountException("amount is required");
        return new BigDecimal(raw.trim());
    }

    private Map<String, String> readBody(HttpExchange ex) throws IOException {
        byte[] bytes = ex.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            throw new InvalidRequestException("request body too large");
        }
        return Json.parseObject(new String(bytes, StandardCharsets.UTF_8));
    }

    private Map<String, Object> userMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id());
        m.put("username", u.username());
        m.put("name", u.displayName());
        m.put("role", u.role().name());
        return m;
    }

    private String authJson(AuthResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", r.token());
        m.put("user", userMap(r.user()));
        return Json.toJson(m);
    }

    private Map<String, Object> accountMap(Account a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("owner", a.getOwner());
        m.put("label", a.getLabel());
        m.put("balance", Money.toDecimal(a.getBalanceMinor()));
        m.put("createdAt", a.getCreatedAt().toString());
        return m;
    }

    private Map<String, Object> transactionMap(Transaction t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.id());
        m.put("type", t.type().name());
        m.put("from", t.fromAccountId());
        m.put("to", t.toAccountId());
        m.put("amount", Money.toDecimal(t.amountMinor()));
        m.put("createdAt", t.createdAt().toString());
        String key = t.idempotencyKey();
        m.put("idempotencyKey", key == null ? null : key.substring(key.indexOf(':') + 1));
        return m;
    }

    private String transactionJson(Transaction t) {
        return Json.toJson(transactionMap(t));
    }

    private String error(String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", code);
        m.put("message", message);
        return Json.toJson(m);
    }

    private void sendHtml(HttpExchange ex, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
