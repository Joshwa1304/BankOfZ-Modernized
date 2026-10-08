package com.bankofz.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** BNKSTMT monthly statement for one account. */
public record StatementDto(
        String sortCode,
        Long accId,
        String accType,
        String customerName,
        LocalDate periodFrom,
        LocalDate periodTo,
        List<TransactionHistoryDto> transactions,
        BigDecimal openingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal closingBalance,
        BigDecimal availableBalance) {

    public int transactionCount() {
        return transactions.size();
    }
}
