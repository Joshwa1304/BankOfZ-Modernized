# Business Rule Traceability

| # | Rule | Legacy location | Java implementation | Verified by |
|---|---|---|---|---|
| BR-01 | Unknown customer on login → `CUSTOMER DOES NOT EXIST` | IBLOGIN1 `LOGIN` (DBSTAT GB/GE) | `AuthService.login` → `CustomerNotFoundException` | `AuthServiceTest.loginUnknownCustomerFails`, `BankOfZEndToEndTest.loginBusinessRules` |
| BR-02 | Password mismatch → `PASSWORD INVALID` (now BCrypt) | IBLOGIN1 `IF PASSWD-SEG NOT = IN-PASSWD` | `AuthService.login` → `InvalidPasswordException` | `AuthServiceTest.loginWithBadPasswordFailsAndLeavesStatusUnchanged` |
| BR-03 | Password checked before logged-in status | IBLOGIN1 nesting order | `AuthService.login` | `AuthServiceTest.passwordIsCheckedBeforeLoggedInStatus` |
| BR-04 | STATUS = '1' → `CUSTOMER ALREADY LOGGED IN` | IBLOGIN1 `IF STATUS-SEG = LOGGEDIN` | `AuthService.login` → `CustomerAlreadyLoggedInException` (see TD-05) | `AuthServiceTest.loginWhileSessionActiveFails`, end-to-end |
| BR-05 | Successful login sets STATUS = '1' and LASTLOGIN = now; reply `LOGIN SUCCESSFUL` | IBLOGIN1 `REPL` | `AuthService.login` | `AuthServiceTest.loginSetsStatusAndLastLoginAndReturnsToken` |
| BR-06 | Logout sets STATUS = '0'; reply `LOGOFF SUCCESSFUL` | IBLOGOUT `LOGOUT` | `AuthService.logout` | `AuthServiceTest.logoutResetsStatus`, end-to-end |
| BR-07 | Logout of unknown customer → `FAILED UPDATE FOR LOGOFF` | IBLOGOUT (GB/GE) | `AuthService.logout` | `AuthServiceTest.logoutUnknownCustomerUsesLegacyMessage` |
| BR-08 | Customer inquiry returns name, address, city, state, zip, phone | IBGCUDAT | `CustomerService.getCustomer`, `CustomerMapper.toDto` | end-to-end |
| BR-09 | Customer update overwrites the seven fields | IBSCUDAT `MOVE ...-IN TO ...-CD` + `REPL` | `CustomerService.updateCustomer`, `CustomerMapper.updateCustomer` | end-to-end |
| BR-10 | Account summary lists the customer's accounts (id, type, balance), max 6 | IBACSUM `OCCURS 1 TO 6` | `AccountService.getAccountSummary` (`bankofz.account.summary-max-size`) | `AccountServiceTest.summaryIsCappedAtConfiguredMaximum` |
| BR-11 | No CUSTACCS rows → `CUSTOMER DOES NOT EXIST` | IBACSUM (GB/GE, TOTAL-ACCS < 1) | `AccountService.getAccountSummary` | `AccountServiceTest.customerWithoutAccountsIsReportedAsMissing` |
| BR-12 | Trx type must be d/w (case-insensitive) else `INVALID ACCOUNT ACTION. MUST BE 'w' OR 'd'.` | IBTRAN `ACCOUNT-ACTIVITY` | `TransactionService.parseTrxType` | `TransactionServiceTest.invalidTypeIsRejectedBeforeAnyDatabaseAccess`, `upperCaseTypesAreAcceptedLikeIbtran` |
| BR-13 | Trx type validated before the account is read | IBTRAN | `TransactionService.post` | `TransactionServiceTest.invalidTypeIsRejectedBeforeAnyDatabaseAccess` |
| BR-14 | Unknown account → `ACCOUNT DOES NOT EXIST` | IBTRAN (GB/GE) | `AccountNotFoundException` | `TransactionServiceTest.unknownAccountFails` |
| BR-15 | `TXID = ACCID * 10^10 + LASTTXID + 1`; LASTTXID incremented | IBTRAN `MULT-FACTOR` | `TransactionIds.next`, `TransactionService.post` | `TransactionServiceTest.transactionIdFollowsLegacyAlgorithm`, end-to-end (txid 1010000000002) |
| BR-16 | Deposit adds, withdrawal subtracts; amounts at V99 precision | IBTRAN `COMPUTE BALANCE-ACC` | `TransactionService.post` (`BigDecimal`, scale 2, truncation) | `depositAddsToBalanceAndWritesHistory`, `withdrawalSubtractsFromBalance`, `extraDecimalsAreTruncatedLikeCobolV99` |
| BR-17 | History row records type, amount, REFTXID = 0, account, resulting balance | IBTRAN `HISTORY-SEG` + JNI `InsertHist` | `TransactionService.post` | `depositAddsToBalanceAndWritesHistory` |
| BR-18 | IBTRAN reply = new balance + customer's account summary | IBTRAN `GET-ACCOUNT-SUMMARY` | `TransactionResponse.accounts` | end-to-end |
| BR-19 | Statement period = 1st to last day of DATECARD month; month-to-date if no DATECARD | BNKSTMT `GET_STATEMENT_PERIOD` | `StatementService.Period` / `resolvePeriod` (leap years fixed) | `StatementServiceTest.periodEndsOnTheRealLastDayOfMonth`, `periodAcceptsDatecardAndIsoFormatsAndDefaultsToMonthToDate` |
| BR-20 | Sort code from SORTCODE DD, default `123456` | BNKSTMT | `StatementService.resolveSortCode`, job parameter `sortCode` | `StatementServiceTest.sortCodeDefaultsLikeBnkstmt` |
| BR-21 | Statement totals: credits, debits, opening, closing, available balance, count | BNKSTMT `PROCESS_TRANSACTIONS`, `PRINT_SUMMARY` | `StatementService.generate` (opening balance corrected, TD-07) | `StatementServiceTest.openingBalanceRollsBackEveryMovementSinceThePeriodStarted`, end-to-end |
| BR-22 | IMS extracts load in key order; field order per UNSTRING | LOADCUST / LOADACCT / LOADCUSA / LOADHIST / LOADTSTA | `ImsDataMigrationJobConfig` | `BankOfZEndToEndTest.migrationLoadsEveryImsSegment` |
| — | Dormant rule `IF CA-CUSTID < 16 MOVE 0 TO BALANCE-ACC` | IBACSUM `RULES-CHECK` (never PERFORMed) | **Not migrated** — see SME-01 | — |
