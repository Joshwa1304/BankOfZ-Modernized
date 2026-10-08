package com.bankofz.exception;

import org.springframework.http.HttpStatus;

/** IMS status GB/GE on the CUSTOMER or CUSTACCS database. */
public class CustomerNotFoundException extends BankOfZException {

    public CustomerNotFoundException() {
        this(LegacyMessages.NO_CUSTOMER);
    }

    public CustomerNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
