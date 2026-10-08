# Transformation Decisions

## Decisions taken

| ID | Decision | Rationale |
|---|---|---|
| TD-01 | **Dual write eliminated.** IBTRAN wrote HISTORY to IMS and a copy to DB2 through a JNI call (`InsertHist`) with no shared commit scope. The rewrite does one insert into `transaction_history` in the same `@Transactional` unit as the account update. | Removes the IMS/DB2 consistency risk and the JNI bridge. |
| TD-02 | **Passwords hashed with BCrypt.** The IMS PASSWORD field held 16 bytes of plaintext. The migration job hashes on load; login uses `PasswordEncoder.matches`. | Security remediation. |
| TD-03 | **Session = JWT + STATUS.** Login issues a JWT. The filter only accepts a token while the customer's STATUS is still '1', so logout invalidates it immediately — the IMS semantics. | Keeps the legacy session model while adding real authentication. |
| TD-04 | **Ownership checks added.** The IMS transactions accepted any CUSTID/ACCID. Customers may now only reach their own data and accounts; teller customers (`CUSTTYPE = 't'`) may reach all. | A public REST API cannot rely on terminal security. |
| TD-05 | **Stale sessions.** With the legacy rule alone, a customer whose token expired could never log in again (logout requires a token). Login is now only refused while the previous login is still within the token lifetime (`bankofz.jwt.expiration-minutes`). | Prevents permanent lock-out. |
| TD-06 | **Leap-year bug fixed.** BNKSTMT hard-coded `LAST_DAY = '28'` for February. Now `YearMonth.lengthOfMonth()`. | Defect fix. |
| TD-07 | **Opening balance fixed.** BNKSTMT computed opening = current balance + period debits − period credits, which is wrong for any month other than the current one. Now opening = current balance − all movements since the period start. Same result for the current month. | Defect fix. |
| TD-08 | **DATECARD / SORTCODE → job parameters** `period` and `sortCode`; default sort code is configurable (`bankofz.statement.default-sort-code`). | Removes hard-coded values. |
| TD-09 | **Summary cap configurable** (`bankofz.account.summary-max-size`, default 6 = `OCCURS 1 TO 6`). | Limit was a message-size constraint, not a business rule. |
| TD-10 | **Transaction type normalised to lower case** when stored. IBTRAN stored `D`/`W` as typed, which the TTYPE lookup does not contain. | Referential integrity with `transaction_type`. |
| TD-11 | **Amount must be positive** (`@DecimalMin("0.01")`, max 13 integer digits). IBTRAN used `NUMVAL` without checks. | Input validation; a negative "deposit" was possible before. |
| TD-12 | **IMS GHU → pessimistic row lock** (`findByIdForUpdate`) on account and customer updates. | Same lost-update protection as IMS hold calls. |
| TD-13 | **IMS status codes other than GB/GE** (`BAD STATUS CODE WAS RECEIVED`) become database exceptions → HTTP 500 with rollback. | No partial updates; the legacy code could REPL after a failed ISRT. |
| TD-14 | **MFS/terminal output replaced** by JSON; legacy MSG-OUT texts are kept verbatim in the `detail` of RFC 7807 error responses. | Clients and test scripts can match the old messages. |
| TD-15 | **Statement customer data** comes from the IMS-side customer (first holder of the account via CUSTACCS). BNKSTMT read the CICS-side DB2 CUSTOMER table (title, DOB, credit score...), which is outside this application's data. | The IMS application is the system being migrated. |

## Items requiring SME confirmation

| ID | Item | Current implementation |
|---|---|---|
| SME-01 | Dormant rule `IF CA-CUSTID < 16 MOVE 0 TO BALANCE-ACC` in IBACSUM (`RULES-CHECK`, never PERFORMed). | Dropped as stale test code. Confirm. |
| SME-02 | Transfer (`TTYPE 't'`) exists in the lookup but IBTRAN rejects it. | Rejected with the legacy message. If transfers are needed, a debit/credit pair with `REFTXID` linking must be specified. |
| SME-03 | MFS screens were not supplied. | REST API only. |
| SME-04 | BNKSTMT reads DB2 `PROCTRAN` / `ACCOUNT` keyed by sort code (CICS-side model). | Mapped to `transaction_history` / `account`; sort code is a report header parameter only. |
| SME-05 | Overdrafts: IBTRAN lets a withdrawal drive the balance negative. | Preserved (no overdraft check). Confirm whether a limit is required. |
| SME-06 | Ownership rule (TD-04) and teller privileges. | Customers: own data only; tellers: everything. Confirm teller scope. |
| SME-07 | HISTORY.data amounts are 10,000× the account balances (e.g. 88300000.00 vs 8830.00). | Loaded as-is. Check the extract format. |
