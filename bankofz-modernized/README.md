# Bank of Z — Modernized (Java 21 + Spring Boot 3)

Rewrite of the Friendly Bank / Bank of Z IMS application (COBOL + PL/I + HLASM DBD/PSB +
IMS DB/TM + DB2 + JNI bridge) as a Spring Boot REST service on PostgreSQL.

| | |
|---|---|
| Language / framework | Java 21, Spring Boot 3.3, Spring Batch 5, Spring Security 6 |
| Security | BCrypt password hashing, JWT bearer tokens, per-customer ownership checks |
| Persistence | PostgreSQL 16, Flyway, Spring Data JPA (pessimistic locking = IMS `GHU`) |
| Mapping | MapStruct, Lombok |
| Build / deploy | Maven, Docker, docker-compose |

## Project structure

```
bankofz-modernized/
├─ pom.xml
├─ Dockerfile
├─ docker-compose.yml
├─ data/ims/                         IMS extracts (CUSTOMER.data, ACCOUNT.data, ...) for the migration job
├─ docs/                             mapping, traceability, decisions, validation report
└─ src/
   ├─ main/java/com/bankofz/
   │  ├─ BankOfZApplication.java
   │  ├─ batch/                      ImsDataMigrationJobConfig (LOAD*), StatementGenerationJobConfig (BNKSTMT)
   │  ├─ config/                     SecurityConfig
   │  ├─ controller/                 Auth, Customer, Account, Transaction REST controllers
   │  ├─ domain/                     Customer, Account, CustomerAccount, TransactionHistory, TransactionIds
   │  │  └─ enums/                   LoginStatus, CustomerType, AccountType, TransactionType
   │  ├─ dto/                        request/response records
   │  ├─ exception/                  domain exceptions, legacy MSG-OUT texts, GlobalExceptionHandler
   │  ├─ mapper/                     CustomerMapper (MapStruct)
   │  ├─ repository/                 Spring Data JPA repositories
   │  ├─ security/                   JwtTokenProvider, JwtAuthenticationFilter, AccessGuard
   │  └─ service/                    AuthService, CustomerService, AccountService, TransactionService, StatementService
   ├─ main/resources/
   │  ├─ application.yml
   │  └─ db/migration/               V1__initial_schema.sql, V2__seed_lookups.sql
   └─ test/                          unit tests + end-to-end test on embedded PostgreSQL
```

## Build and test

```bash
mvn clean package
```

The test suite needs no external database: `BankOfZEndToEndTest` starts an embedded
PostgreSQL 16, applies the Flyway scripts, migrates `data/ims` and drives the API.

## Run locally

```bash
docker-compose up --build
```

Or against your own PostgreSQL:

```bash
java -jar target/bankofz-modernized-1.0.0.jar --spring.datasource.url=jdbc:postgresql://localhost:5432/bankofz
```

Set `BANKOFZ_JWT_SECRET` (at least 32 bytes) for anything other than local development.

## Migrate the IMS data

Runs LOADCUST → LOADACCT → LOADCUSA → LOADHIST → LOADTSTA as one job. Plaintext IMS
passwords are BCrypt-hashed during the load. The job is re-runnable.

```bash
java -jar target/bankofz-modernized-1.0.0.jar --spring.batch.job.enabled=true --spring.batch.job.name=imsDataMigrationJob inputDir=data/ims
```

## Generate monthly statements (BNKSTMT)

`period` replaces the DATECARD DD (yyyyMM or yyyy-MM; omit for month-to-date),
`sortCode` replaces the SORTCODE DD (default `123456`). One file per account.

```bash
java -jar target/bankofz-modernized-1.0.0.jar --spring.batch.job.enabled=true --spring.batch.job.name=statementGenerationJob period=202606 sortCode=123456 outputDir=statements
```

## REST API

All endpoints except `POST /auth/login` need `Authorization: Bearer <token>`. Customers
can only reach their own data and accounts; teller customers (`custType = t`) can reach all.

| Method | Path | Legacy | Notes |
|---|---|---|---|
| POST | `/auth/login` | IBLOGIN1 | `{"custId":1,"password":"password"}` → token |
| POST | `/auth/logout` | IBLOGOUT | resets STATUS to '0'; the token stops working |
| GET | `/customers/{custId}` | IBGCUDAT | |
| PUT | `/customers/{custId}` | IBSCUDAT | full replace of name, address, city, state, zip, phone |
| GET | `/customers/{custId}/accounts` | IBACSUM | max 6 by default (`bankofz.account.summary-max-size`) |
| GET | `/accounts/{accId}` | — | single account |
| POST | `/accounts/{accId}/transactions` | IBTRAN | `{"trxType":"d","amount":100.00}`; returns balance + account summary |
| GET | `/accounts/{accId}/transactions?limit=50` | IMS JMP history query | newest first |
| GET | `/accounts/{accId}/statements/{period}` | BNKSTMT | on-demand statement as JSON |

Errors are RFC 7807 problem details whose `detail` keeps the legacy MSG-OUT text,
e.g. `CUSTOMER DOES NOT EXIST`, `PASSWORD INVALID`, `CUSTOMER ALREADY LOGGED IN`.

## Documentation

- [docs/source-target-mapping.md](docs/source-target-mapping.md)
- [docs/business-rule-traceability.md](docs/business-rule-traceability.md)
- [docs/transformation-decisions.md](docs/transformation-decisions.md) — includes the items needing SME confirmation
- [docs/validation-report.md](docs/validation-report.md)
