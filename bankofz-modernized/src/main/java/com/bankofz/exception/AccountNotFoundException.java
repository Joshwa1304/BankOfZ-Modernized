package com.bankofz.exception;

import org.springframework.http.HttpStatus;

/** IMS status GB/GE on the ACCOUNT database. */
public class AccountNotFoundException extends BankOfZException {

    public AccountNotFoundException() {
        super(LegacyMessages.NO_ACCOUNT, HttpStatus.NOT_FOUND);
    }
}
