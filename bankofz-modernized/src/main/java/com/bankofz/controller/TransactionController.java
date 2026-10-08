package com.bankofz.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bankofz.dto.TransactionHistoryDto;
import com.bankofz.dto.TransactionRequest;
import com.bankofz.dto.TransactionResponse;
import com.bankofz.security.AccessGuard;
import com.bankofz.service.TransactionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** IMS transaction IBTRAN (deposit / withdrawal) and transaction history. */
@RestController
@RequestMapping("/accounts/{accId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AccessGuard accessGuard;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse post(@PathVariable Long accId,
                                    @Valid @RequestBody TransactionRequest request,
                                    Authentication authentication) {
        accessGuard.checkAccount(authentication, accId);
        return transactionService.post(accId, request, accessGuard.currentCustomerId(authentication));
    }

    @GetMapping
    public List<TransactionHistoryDto> history(@PathVariable Long accId,
                                               @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit,
                                               Authentication authentication) {
        accessGuard.checkAccount(authentication, accId);
        return transactionService.getHistory(accId, limit);
    }
}
