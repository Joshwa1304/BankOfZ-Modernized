# Validation Report

Build: `mvn clean package` — **BUILD SUCCESS**, Java 21.0.12, Maven 3.9.11.

## Test results

| Test class | Kind | Tests | Result |
|---|---|---|---|
| `AuthServiceTest` | unit (Mockito) | 8 | pass |
| `TransactionServiceTest` | unit (Mockito, parameterised) | 11 | pass |
| `AccountServiceTest` | unit (Mockito) | 2 | pass |
| `StatementServiceTest` | unit (Mockito, parameterised) | 9 | pass |
| `BankOfZEndToEndTest` | end-to-end, embedded PostgreSQL 16 | 4 | pass |
| **Total** | | **34** | **34 pass, 0 fail** |

## End-to-end coverage (`BankOfZEndToEndTest`)

Runs against a real PostgreSQL 16 (zonky embedded-postgres, no Docker):

1. Flyway applies `V1__initial_schema.sql` and `V2__seed_lookups.sql`; Hibernate `ddl-auto: validate` passes.
2. `imsDataMigrationJob` loads the original extracts from `data/ims`:

   | Table | Rows |
   |---|---|
   | customer | 100 |
   | account | 265 |
   | customer_account | 265 |
   | transaction_history | 265 |
   | transaction_status | 265 |

   All foreign keys resolve; passwords are stored as BCrypt hashes.
3. Customer 1 (Martha Antonelli) logs in with the migrated password, reads profile and
   account summary (accounts 101, 102), deposits 100.00 to account 101
   (txid `1010000000002`, balance 8830.00 → 8930.00), withdraws 30.50 with type `W`
   (txid `1010000000003`, balance 8899.50), is refused a transfer (`t`) with the legacy
   message, reads history and the month statement (opening 8830.00, credits 100.00,
   debits 30.50, closing 8899.50), updates the profile, and is denied access to
   customer 2 and account 201 (403).
4. Logout → old token is rejected (401) → login works again.
5. Login rules: unknown customer 404, bad password 401, second login 409, no token 401.
6. `statementGenerationJob` for DATECARD `200307` writes 265 statement files with the
   period `2003-07-01 TO 2003-07-31`.

## Not yet validated

- Parallel run / output reconciliation against the live IMS system.
- Load and concurrency testing of the pessimistic-locking path.
- The Docker image build (Docker was not available in the build environment).
