package com.bankofz.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankofz.dto.AccountSummaryDto;
import com.bankofz.dto.StatementDto;
import com.bankofz.security.AccessGuard;
import com.bankofz.service.AccountService;
import com.bankofz.service.StatementService;

import lombok.RequiredArgsConstructor;

/** IMS transaction IBACSUM, single-account lookup and on-demand BNKSTMT statements. */
@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final StatementService statementService;
    private final AccessGuard accessGuard;

    @GetMapping("/customers/{custId}/accounts")
    public List<AccountSummaryDto> getAccountSummary(@PathVariable Integer custId, Authentication authentication) {
        accessGuard.checkCustomer(authentication, custId);
        return accountService.getAccountSummary(custId);
    }

    @GetMapping("/accounts/{accId}")
    public AccountSummaryDto getAccount(@PathVariable Long accId, Authentication authentication) {
        accessGuard.checkAccount(authentication, accId);
        return accountService.getAccount(accId);
    }

    /** @param period statement month as yyyy-MM or yyyyMM (DATECARD format). */
    @GetMapping("/accounts/{accId}/statements/{period}")
    public StatementDto getStatement(@PathVariable Long accId,
                                     @PathVariable String period,
                                     @RequestParam(required = false) String sortCode,
                                     Authentication authentication) {
        accessGuard.checkAccount(authentication, accId);
        return statementService.generate(accId, statementService.resolvePeriod(period), sortCode);
    }
}
