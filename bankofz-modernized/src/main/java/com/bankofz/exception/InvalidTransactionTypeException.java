package com.bankofz.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransactionTypeException extends BankOfZException {

    public InvalidTransactionTypeException() {
        super(LegacyMessages.INVALID_TRX_TYPE, HttpStatus.BAD_REQUEST);
    }
}
