package com.bankofz.exception;

import org.springframework.http.HttpStatus;

/** Bad DATECARD-style period passed to statement generation. */
public class InvalidStatementPeriodException extends BankOfZException {

    public InvalidStatementPeriodException(String period) {
        super("Statement period must be yyyy-MM or yyyyMM: " + period, HttpStatus.BAD_REQUEST);
    }
}
