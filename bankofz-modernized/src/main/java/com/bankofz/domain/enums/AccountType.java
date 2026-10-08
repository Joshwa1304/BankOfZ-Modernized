package com.bankofz.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** ACCTYPE lookup database. */
public enum AccountType {
    CHECKING("c"),
    SAVINGS("s");

    private final String code;

    AccountType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AccountType fromCode(String code) {
        for (AccountType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown account type: " + code);
    }

    @Converter
    public static class JpaConverter implements AttributeConverter<AccountType, String> {
        @Override
        public String convertToDatabaseColumn(AccountType value) {
            return value == null ? null : value.code;
        }

        @Override
        public AccountType convertToEntityAttribute(String code) {
            return code == null ? null : fromCode(code);
        }
    }
}
