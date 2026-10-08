# Source → Target Mapping

## Programs

| Legacy source | Type | Java target |
|---|---|---|
| `IBLOGIN1` / `IBLOGIN` | COBOL IMS MPP | `AuthController.login` → `AuthService.login` |
| `IBLOGOUT` | COBOL IMS MPP | `AuthController.logout` → `AuthService.logout` |
| `IBGCUDAT` | COBOL IMS MPP | `CustomerController.getCustomer` → `CustomerService.getCustomer` |
| `IBSCUDAT` | COBOL IMS MPP | `CustomerController.updateCustomer` → `CustomerService.updateCustomer` |
| `IBACSUM` | COBOL IMS MPP | `AccountController.getAccountSummary` → `AccountService.getAccountSummary` |
| `IBTRAN` | COBOL IMS MPP + JNI | `TransactionController.post` → `TransactionService.post` |
| `nazare.jmp.controller.InsertHist` (JNI target) | Java on IMS JMP | removed — single insert in `TransactionService.post` |
| `nazare.jmp.*` history query (`QueryTransaction`, `IMSBankHistory`) | Java on IMS JMP | `TransactionController.history` → `TransactionService.getHistory` |
| `BNKSTMT` | PL/I batch + DB2 | `StatementService` + `StatementGenerationJobConfig` (job `statementGenerationJob`) |
| `LOADCUST` | COBOL IMS BMP | `ImsDataMigrationJobConfig.loadCustomerStep` |
| `LOADACCT` | COBOL IMS BMP | `ImsDataMigrationJobConfig.loadAccountStep` |
| `LOADCUSA` | COBOL IMS BMP | `ImsDataMigrationJobConfig.loadCustomerAccountStep` |
| `LOADHIST` | COBOL IMS BMP | `ImsDataMigrationJobConfig.loadHistoryStep` |
| `LOADTSTA` | COBOL IMS BMP | `ImsDataMigrationJobConfig.loadTransactionStatusStep` |
| DBD / PSB (HLASM) | IMS definitions | `V1__initial_schema.sql` + JPA entities |
| MFS screens | IMS TM | not supplied — replaced by the REST API |

## Data (IMS segment → PostgreSQL table)

| IMS DBD / segment | Fields (bytes) | Table | Entity |
|---|---|---|---|
| CUSTOMER | CUSTID(4) LASTNAME(50) FIRSTNM(50) ADDRESS(80) CITY(25) STATE(2) ZIPCODE(15) PHONE(12) STATUS(1) PASSWORD(16) CUSTTYPE(1) LASTLOGI(23) | `customer` | `Customer` |
| ACCOUNT | ACCID(8) ACCTYPE(1) BALANCE(8, COMP-3) LASTTXID(8) | `account` | `Account` |
| CUSTACCS | CUSTID(4) ACCID(8) ACCNUM(4) | `customer_account` | `CustomerAccount` |
| HISTORY | TXID(8) TIME(23) TRANSTYP(1) AMOUNT(8, COMP-3) REFTXID(8) ACCID(8) | `transaction_history` | `TransactionHistory` |
| TSTAT | TXID(8) STATUS(1) STARTTIM(23) STOPTIME(23) CUSTID(4) | `transaction_status` | — (loaded by batch only) |
| ACCTYPE | CODE DESCRIPT | `account_type` | `AccountType` enum |
| CUSTTYPE | CODE DESCRIPT | `customer_type` | `CustomerType` enum |
| TTYPE | CODE DESCRIPT | `transaction_type` | `TransactionType` enum |
| TSTATTYP | CODE DESCRIPT | `transaction_status_type` | — |
| DB2 `PROCTRAN` (BNKSTMT) | | `transaction_history` | `TransactionHistory` |

## Data types

| COBOL / PL/I | PostgreSQL | Java |
|---|---|---|
| `PIC S9(9) COMP-5` | `INTEGER` | `Integer` |
| `PIC S9(18) COMP-5` | `BIGINT` | `Long` |
| `PIC S9(13)V9(2) COMP-3` | `NUMERIC(15,2)` | `BigDecimal` |
| `PIC X(n)` | `VARCHAR(n)` | `String` / enum |
| 23-byte timestamp text `YYYY-MM-DD HH:MM:SS.ff0` | `TIMESTAMP` | `LocalDateTime` |

## IMS calls

| DL/I call | Replacement |
|---|---|
| `GU` / `GN` | `findById` / query methods |
| `GHU` (get hold) | `findByIdForUpdate` with `PESSIMISTIC_WRITE` lock |
| `GHN` loop over CUSTACCS | `findByIdCustIdOrderByAccNumAsc` |
| `REPL` | JPA dirty checking inside `@Transactional` |
| `ISRT` (database) | `repository.save` / batch `INSERT` |
| `ISRT` to IOPCB (reply) | REST response body |
| `GU` from IOPCB (input message) | REST request body / path |
