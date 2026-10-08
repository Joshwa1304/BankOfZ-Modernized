package com.bankofz.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** IBTRAN output message: new balance plus the customer's account summary. */
public record TransactionResponse(
        Long txId,
        Long accId,
        String trxType,
        BigDecimal amount,
        BigDecimal balance,
        LocalDateTime txTime,
        List<AccountSummaryDto> accounts) {
}
