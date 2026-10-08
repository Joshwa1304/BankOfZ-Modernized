package com.bankofz.exception;

import org.springframework.http.HttpStatus;

/** Base class for business errors that map to a legacy MSG-OUT value. */
public abstract class BankOfZException extends RuntimeException {

    private final HttpStatus status;

    protected BankOfZException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
