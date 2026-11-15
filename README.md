# PayFlow: Digital Wallet and Transaction Service

A backend service written in **core Java (no frameworks)** with a built-in web app. Users register, log in, create their own wallet accounts, and move money safely. It is built around the problems real payment systems have to solve: **correct money handling, atomic transfers, safe concurrency, duplicate-request protection, and per-user access control**.

## Features
- **Register and log in** with a username and password; every user sees and controls only their own accounts
- Create accounts (up to 5 per user), deposit, withdraw, and send money to any account by its ID
- Transaction history per account
- **Atomic transfers**: debit, credit, and record happen together or not at all
- **Thread-safe**: per-account locks, with ordered locking so opposite-direction transfers can never deadlock
- **Idempotent transfers**: retrying a request with the same `idempotencyKey` does not move money twice
- **Exact money arithmetic**: amounts are stored as `long` minor units (paise), never `double`
- **Bank owner console**: a separate admin login that shows every customer, every account, total money held, and the full transaction history with who did what (searchable, filterable, CSV export, live updates)
- **Web dashboard** with login and register pages, bank-card style accounts, animated balances, history, and a concurrency stress test
- Clean error handling with proper HTTP status codes (400, 401, 403, 404, 405, 409, 422, 429)
- Repository interfaces, so the in-memory storage can be swapped for MySQL/JDBC without changing business logic

## Security design
| Concern | What the code does |
|---|---|
| Password storage | PBKDF2 (HMAC-SHA256), random salt per password, 120,000 iterations, constant-time comparison |
| Sessions | Random 256-bit token sent as `Authorization: Bearer ...`; expires after 2 hours; logout invalidates it |
| Account takeover by guessing | 5 failed logins lock that username for 1 minute; the same generic error is returned for "unknown user" and "wrong password" |
| Access control | Every account endpoint checks the logged-in user owns the account (403 otherwise); you can only send money out of your own accounts |
| Replay across users | Idempotency keys are scoped per user |
| Bank owner access | The owner account is created at server start-up and cannot be obtained by registering; admin routes return 403 to everyone else; the password is random per run unless you set `PAYFLOW_ADMIN_PASSWORD` |
| Abuse | Request bodies are capped at 64 KB; at most 5 accounts per user |

This is a learning/demo project. For production it would also need HTTPS, persistent sessions, email verification, and a real database.

## Architecture
```
api/          ApiServer        HTTP routing, JSON in/out, auth check, error to status mapping (JDK HttpServer)
              Dashboard        the single-page web app (HTML/CSS/JS with inline SVG art)
service/      WalletService    money rules, locking, idempotency, ownership checks, consistent snapshots
              AuthService      registration, login, sessions, lockout, owner creation
              AdminService     platform totals, all accounts/customers/transactions for the owner
repository/   interfaces + in-memory implementations (swap point for a database)
model/        User (with Role), Account, Transaction
exception/    typed business errors with stable error codes
util/         Money (exact amounts), Json (small parser/writer), PasswordHasher
```

## Design decisions
| Problem | Decision |
|---|---|
| Floating point errors with money | Store `long` minor units; use `BigDecimal` only at the edges |
| Two transfers touching the same accounts at once | Per-account `ReentrantLock`; credit/debit refuse to run without the lock held |
| Deadlock (A to B and B to A at the same time) | Always acquire locks in sorted account-id order |
| Client retries after a timeout | `idempotencyKey` returns the original transaction; a reused key with different data returns 409 |
| Replacing storage later | `AccountRepository` / `TransactionRepository` / `UserRepository` interfaces |

## Run it
Requires **JDK 17 or newer**.
```bash
./run.sh test      # compile + run the 27 tests   (Windows: run.bat test)
./run.sh           # start the server
```
Then open **http://localhost:8080** in your browser, register, and try it. In VS Code you can also just open `Main.java` and press Run.

### Bank owner login
When the server starts it prints the owner login in the terminal:
```
=== Bank owner login ===
Username: owner
Password: <random, shown once per run>
```
Log in with those on the same page to open the owner console. To choose your own password, set the environment variables `PAYFLOW_ADMIN_USER` and `PAYFLOW_ADMIN_PASSWORD` (at least 8 characters) before starting.

To demo sending money between people, register a second user in a private/incognito window and send money to that user's account ID.

## API
All routes except `/`, `/auth/register`, and `/auth/login` need the header `Authorization: Bearer <token>`.

| Method | Path | Body |
|---|---|---|
| GET | `/` | the web app |
| POST | `/auth/register` | `{"username":"asha","password":"password1","name":"Asha Kumar"}` |
| POST | `/auth/login` | `{"username":"asha","password":"password1"}` |
| POST | `/auth/logout` | |
| GET | `/auth/me` | |
| GET | `/accounts` | your accounts |
| POST | `/accounts` | `{"label":"Savings","initialBalance":500}` |
| GET | `/accounts/{id}` | |
| POST | `/accounts/{id}/deposit` | `{"amount":100.50}` |
| POST | `/accounts/{id}/withdraw` | `{"amount":20}` |
| GET | `/accounts/{id}/transactions` | |
| GET | `/lookup/{id}` | recipient's public name only |
| GET | `/admin/overview` | owner only: customers, accounts, total money, totals by type, ledger check |
| GET | `/admin/transactions?limit=500` | owner only: all transactions with who did what, newest first |
| GET | `/admin/accounts` | owner only: every account with owner and balance |
| GET | `/admin/users` | owner only: every customer with account count and total balance |
| POST | `/transfers` | `{"from":"ACC-1001","to":"ACC-1002","amount":125.50,"idempotencyKey":"abc-1"}` |

Error format: `{"error":"INSUFFICIENT_FUNDS","message":"Insufficient funds in account ACC-1002"}`

## Tests
27 tests cover money conversion, JSON parsing, deposits and withdrawals, validation, atomic and failed transfers, idempotency, a **deadlock test** (40,000 opposite-direction transfers), a **concurrency test** (8 threads, 24,000 random transfers, total money must stay constant and no balance may go negative), password hashing, registration and login, lockout, session expiry, ownership checks, owner-only access, owner totals and history, a **snapshot consistency test** (the owner's totals are read repeatedly while thousands of deposits, withdrawals, transfers and account creations run, and must always balance), and an end-to-end REST flow over real HTTP with two users and the owner.

## Possible next steps
- MySQL persistence through JDBC implementations of the repository interfaces (with real database transactions)
- Migrate the tests to JUnit 5 and build with Maven
- HTTPS, pagination of history, and Docker packaging
