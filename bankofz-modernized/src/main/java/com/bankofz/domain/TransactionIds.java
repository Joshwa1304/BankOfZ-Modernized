package com.bankofz.domain;

/** IBTRAN transaction-id algorithm. */
public final class TransactionIds {

    /** MULT-FACTOR PIC S9(18) COMP-5 VALUE 10000000000. */
    public static final long MULT_FACTOR = 10_000_000_000L;

    private TransactionIds() {
    }

    /** TXID = ACCID * 10^10 + LASTTXID + 1. */
    public static long next(long accId, long lastTxId) {
        return Math.addExact(Math.multiplyExact(accId, MULT_FACTOR), lastTxId + 1);
    }
}
