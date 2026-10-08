package com.bankofz.dto;

import java.math.BigDecimal;

/** One ACCOUNT-SUMMARY occurrence (BALANCE-AS, ACCTYPE-AS, ACCID-AS). */
public record AccountSummaryDto(Long accId, String accType, BigDecimal balance) {
}
