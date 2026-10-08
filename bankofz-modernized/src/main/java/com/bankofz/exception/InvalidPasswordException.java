package com.bankofz.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends BankOfZException {

    public InvalidPasswordException() {
        super(LegacyMessages.PASSWORD_INVALID, HttpStatus.UNAUTHORIZED);
    }
}
