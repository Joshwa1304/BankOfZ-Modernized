package com.bankofz.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryDto(
        Long txId,
        LocalDateTime txTime,
        String trxType,
        BigDecimal amount,
        Long refTxId,
        BigDecimal balance) {
}
