package com.bankofz.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** TTYPE lookup database. */
public enum TransactionType {
    DEPOSIT("d"),
    WITHDRAWAL("w"),
    TRANSFER("t");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + code);
    }

    @Converter
    public static class JpaConverter implements AttributeConverter<TransactionType, String> {
        @Override
        public String convertToDatabaseColumn(TransactionType value) {
            return value == null ? null : value.code;
        }

        @Override
        public TransactionType convertToEntityAttribute(String code) {
            return code == null ? null : fromCode(code);
        }
    }
}
