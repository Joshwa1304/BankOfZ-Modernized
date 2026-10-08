package com.bankofz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** IBLOGIN1 input message (IN-CUSTID, IN-PASSWD). */
public record LoginRequest(
        @NotNull @Positive Integer custId,
        @NotBlank String password) {
}
