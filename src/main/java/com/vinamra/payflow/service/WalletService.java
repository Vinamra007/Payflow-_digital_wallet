package com.vinamra.payflow.service;

import com.vinamra.payflow.exception.AccountNotFoundException;
import com.vinamra.payflow.exception.ForbiddenException;
import com.vinamra.payflow.exception.IdempotencyConflictException;
import com.vinamra.payflow.exception.InvalidAmountException;
import com.vinamra.payflow.exception.InvalidRequestException;
import com.vinamra.payflow.model.Account;
import com.vinamra.payflow.model.Transaction;
import com.vinamra.payflow.model.Transaction.Type;
import com.vinamra.payflow.repository.AccountRepository;
import com.vinamra.payflow.repository.TransactionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core business logic: accounts, deposits, withdrawals and transfers.
 *
 * Concurrency design:
 *  - every account has its own lock, so unrelated accounts never block each other;
 *  - a transfer locks both accounts in a fixed (sorted by id) order, which makes
 *    deadlock impossible even when A-to-B and B-to-A transfers happen at the same time;
 *  - the debit, credit and transaction record all happen while the locks are held,
 *    so a transfer is all-or-nothing.
 *
 * Idempotency: a transfer can carry a client-supplied key. Replaying the same key
 * returns the original result instead of moving money twice (important for network retries).
 */
public class WalletService {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final AtomicLong txnSequence = new AtomicLong(100000);

    public WalletService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public static final int MAX_ACCOUNTS_PER_USER = 5;
    private final Object createLock = new Object();

    public Account createAccount(String ownerUserId, String ownerName, String label, long initialBalanceMinor) {
        if (ownerUserId == null || ownerUserId.isBlank()) {
            throw new InvalidRequestException("owner is required");
        }
        if (ownerName == null || ownerName.isBlank()) {
            throw new InvalidRequestException("owner name is required");
        }
        if (initialBalanceMinor < 0) {
            throw new InvalidAmountException("initial balance cannot be negative");
        }
        String cleanLabel = label == null || label.isBlank() ? "Wallet" : label.trim();
        if (cleanLabel.length() > 20) {
            throw new InvalidRequestException("label must be at most 20 characters");
        }
        Account account;
        synchronized (createLock) {
            if (accounts.findByUserId(ownerUserId).size() >= MAX_ACCOUNTS_PER_USER) {
                throw new InvalidRequestException("You can have at most " + MAX_ACCOUNTS_PER_USER + " accounts");
            }
            account = accounts.create(ownerUserId, ownerName.trim(), cleanLabel, initialBalanceMinor);
            if (initialBalanceMinor > 0) {
                // saved inside the lock so a snapshot never sees the balance without its transaction
                transactions.save(newTransaction(Type.DEPOSIT, null, account.getId(), initialBalanceMinor, null));
            }
        }
        return account;
    }

    public record AccountBalance(Account account, long balanceMinor) {
    }

    public record LedgerSnapshot(List<AccountBalance> accounts, List<Transaction> transactions) {
    }

    /**
     * A consistent view of the whole platform for the bank owner. It briefly locks every account
     * (in sorted id order, the same order transfers use, so it cannot deadlock) while it reads the
     * balances and the transaction list, so the totals can never show a half-finished transfer.
     */
    public LedgerSnapshot snapshot() {
        synchronized (createLock) { // no account is being created right now
            List<Account> all = accounts.findAll();
            all.sort(Comparator.comparing(Account::getId));
            for (Account a : all) a.lock();
            try {
                List<AccountBalance> balances = new ArrayList<>();
                for (Account a : all) balances.add(new AccountBalance(a, a.getBalanceMinor()));
                return new LedgerSnapshot(balances, transactions.findAll());
            } finally {
                for (int i = all.size() - 1; i >= 0; i--) all.get(i).unlock();
            }
        }
    }

    public Account getAccount(String accountId) {
        return accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    /** Returns the account only if it belongs to the given user; otherwise throws Forbidden. */
    public Account requireOwner(String userId, String accountId) {
        Account account = getAccount(accountId);
        if (!account.getOwnerUserId().equals(userId)) {
            throw new ForbiddenException("You do not own account " + accountId);
        }
        return account;
    }

    public List<Account> listAccounts(String userId) {
        return accounts.findByUserId(userId);
    }

    public Transaction deposit(String accountId, long amountMinor) {
        requirePositive(amountMinor);
        Account account = getAccount(accountId);
        account.lock();
        try {
            account.credit(amountMinor);
            return transactions.save(newTransaction(Type.DEPOSIT, null, accountId, amountMinor, null));
        } finally {
            account.unlock();
        }
    }

    public Transaction withdraw(String accountId, long amountMinor) {
        requirePositive(amountMinor);
        Account account = getAccount(accountId);
        account.lock();
        try {
            account.debit(amountMinor);
            return transactions.save(newTransaction(Type.WITHDRAWAL, accountId, null, amountMinor, null));
        } finally {
            account.unlock();
        }
    }

    public Transaction transfer(String fromId, String toId, long amountMinor, String idempotencyKey) {
        requirePositive(amountMinor);
        if (fromId == null || toId == null) {
            throw new InvalidRequestException("from and to accounts are required");
        }
        if (fromId.equals(toId)) {
            throw new InvalidRequestException("cannot transfer to the same account");
        }
        Account from = getAccount(fromId);
        Account to = getAccount(toId);

        // Always lock in the same order to avoid deadlock.
        Account first = fromId.compareTo(toId) < 0 ? from : to;
        Account second = first == from ? to : from;
        first.lock();
        try {
            second.lock();
            try {
                if (idempotencyKey != null) {
                    Optional<Transaction> existing = transactions.findByIdempotencyKey(idempotencyKey);
                    if (existing.isPresent()) {
                        Transaction t = existing.get();
                        boolean same = t.type() == Type.TRANSFER
                                && fromId.equals(t.fromAccountId())
                                && toId.equals(t.toAccountId())
                                && amountMinor == t.amountMinor();
                        if (!same) {
                            throw new IdempotencyConflictException(idempotencyKey);
                        }
                        return t; // replay: no money moves a second time
                    }
                }
                from.debit(amountMinor);
                to.credit(amountMinor);
                return transactions.save(newTransaction(Type.TRANSFER, fromId, toId, amountMinor, idempotencyKey));
            } finally {
                second.unlock();
            }
        } finally {
            first.unlock();
        }
    }

    public List<Transaction> history(String accountId) {
        getAccount(accountId); // 404 if unknown
        return transactions.findByAccountId(accountId);
    }

    private void requirePositive(long amountMinor) {
        if (amountMinor <= 0) {
            throw new InvalidAmountException("amount must be greater than zero");
        }
    }

    private Transaction newTransaction(Type type, String from, String to, long amount, String key) {
        String id = "TXN-" + txnSequence.incrementAndGet();
        return new Transaction(id, type, from, to, amount, Instant.now(), key);
    }
}
