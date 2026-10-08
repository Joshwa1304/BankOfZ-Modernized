package com.bankofz.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * IBTRAN input message (IN-TRXTYPE, IN-AMOUNT). The account comes from the URL
 * and the customer (IN-CUSTID) from the authenticated user.
 */
public record TransactionRequest(
        @NotBlank String trxType,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount) {
}
