package com.bankofz.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** CUSTTYPE lookup database. */
public enum CustomerType {
    REGULAR("c"),
    TELLER("t");

    private final String code;

    CustomerType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CustomerType fromCode(String code) {
        for (CustomerType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown customer type: " + code);
    }

    @Converter
    public static class JpaConverter implements AttributeConverter<CustomerType, String> {
        @Override
        public String convertToDatabaseColumn(CustomerType value) {
            return value == null ? null : value.code;
        }

        @Override
        public CustomerType convertToEntityAttribute(String code) {
            return code == null ? null : fromCode(code);
        }
    }
}
