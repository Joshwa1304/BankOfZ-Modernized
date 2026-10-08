package com.bankofz.exception;

import org.springframework.http.HttpStatus;

public class CustomerAlreadyLoggedInException extends BankOfZException {

    public CustomerAlreadyLoggedInException() {
        super(LegacyMessages.CUSTOMER_LOGGED_IN, HttpStatus.CONFLICT);
    }
}
