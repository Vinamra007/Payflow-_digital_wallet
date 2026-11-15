package com.vinamra.payflow.service;

import com.vinamra.payflow.model.Account;
import com.vinamra.payflow.model.Role;
import com.vinamra.payflow.model.Transaction;
import com.vinamra.payflow.model.User;
import com.vinamra.payflow.repository.UserRepository;
import com.vinamra.payflow.service.WalletService.AccountBalance;
import com.vinamra.payflow.service.WalletService.LedgerSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only reporting for the bank owner: platform totals, every account, every customer and
 * the full transaction history with who did what. All numbers come from one consistent snapshot.
 */
public class AdminService {
    private final WalletService wallet;
    private final UserRepository users;

    public AdminService(WalletService wallet, UserRepository users) {
        this.wallet = wallet;
        this.users = users;
    }

    public record Overview(int customers, int accounts, long totalBalanceMinor, int transactions,
                           long depositedMinor, long withdrawnMinor, long transferredMinor,
                           int depositCount, int withdrawalCount, int transferCount,
                           long expectedBalanceMinor, boolean ledgerBalanced) {
    }

    public record AccountView(String id, String label, String owner, String username,
                              long balanceMinor, Instant createdAt) {
    }

    public record CustomerView(String name, String username, int accounts, long balanceMinor) {
    }

    public record TransactionView(Transaction transaction, String fromOwner, String fromUsername,
                                  String toOwner, String toUsername) {
    }

    public Overview overview() {
        LedgerSnapshot snap = wallet.snapshot();
        long total = 0;
        for (AccountBalance b : snap.accounts()) total += b.balanceMinor();
        long deposited = 0, withdrawn = 0, transferred = 0;
        int dc = 0, wc = 0, tc = 0;
        for (Transaction t : snap.transactions()) {
            switch (t.type()) {
                case DEPOSIT -> {
                    deposited += t.amountMinor();
                    dc++;
                }
                case WITHDRAWAL -> {
                    withdrawn += t.amountMinor();
                    wc++;
                }
                case TRANSFER -> {
                    transferred += t.amountMinor();
                    tc++;
                }
            }
        }
        int customers = 0;
        for (User u : users.findAll()) if (u.role() == Role.CUSTOMER) customers++;
        // Money only enters via deposits and leaves via withdrawals; transfers just move it around.
        long expected = deposited - withdrawn;
        return new Overview(customers, snap.accounts().size(), total, snap.transactions().size(),
                deposited, withdrawn, transferred, dc, wc, tc, expected, total == expected);
    }

    public List<AccountView> accounts() {
        Map<String, User> byId = usersById();
        List<AccountView> list = new ArrayList<>();
        for (AccountBalance b : wallet.snapshot().accounts()) {
            Account a = b.account();
            list.add(new AccountView(a.getId(), a.getLabel(), a.getOwner(), usernameOf(byId, a.getOwnerUserId()),
                    b.balanceMinor(), a.getCreatedAt()));
        }
        list.sort((x, y) -> x.createdAt().compareTo(y.createdAt()));
        return list;
    }

    public List<CustomerView> customers() {
        Map<String, int[]> accountCount = new HashMap<>();
        Map<String, long[]> balance = new HashMap<>();
        for (AccountBalance b : wallet.snapshot().accounts()) {
            String uid = b.account().getOwnerUserId();
            accountCount.computeIfAbsent(uid, k -> new int[1])[0]++;
            balance.computeIfAbsent(uid, k -> new long[1])[0] += b.balanceMinor();
        }
        List<CustomerView> list = new ArrayList<>();
        for (User u : users.findAll()) {
            if (u.role() != Role.CUSTOMER) continue;
            int n = accountCount.containsKey(u.id()) ? accountCount.get(u.id())[0] : 0;
            long bal = balance.containsKey(u.id()) ? balance.get(u.id())[0] : 0;
            list.add(new CustomerView(u.displayName(), u.username(), n, bal));
        }
        list.sort((x, y) -> x.username().compareToIgnoreCase(y.username()));
        return list;
    }

    /** Newest first, at most {@code limit} entries. */
    public List<TransactionView> transactions(int limit) {
        LedgerSnapshot snap = wallet.snapshot();
        Map<String, User> byId = usersById();
        Map<String, Account> accountById = new HashMap<>();
        for (AccountBalance b : snap.accounts()) accountById.put(b.account().getId(), b.account());

        List<Transaction> all = new ArrayList<>(snap.transactions());
        Collections.reverse(all);
        List<TransactionView> views = new ArrayList<>();
        for (Transaction t : all) {
            if (views.size() >= limit) break;
            Account from = t.fromAccountId() == null ? null : accountById.get(t.fromAccountId());
            Account to = t.toAccountId() == null ? null : accountById.get(t.toAccountId());
            views.add(new TransactionView(t,
                    from == null ? null : from.getOwner(), from == null ? null : usernameOf(byId, from.getOwnerUserId()),
                    to == null ? null : to.getOwner(), to == null ? null : usernameOf(byId, to.getOwnerUserId())));
        }
        return views;
    }

    private Map<String, User> usersById() {
        Map<String, User> map = new HashMap<>();
        for (User u : users.findAll()) map.put(u.id(), u);
        return map;
    }

    private String usernameOf(Map<String, User> byId, String userId) {
        User u = byId.get(userId);
        return u == null ? "" : u.username();
    }
}
