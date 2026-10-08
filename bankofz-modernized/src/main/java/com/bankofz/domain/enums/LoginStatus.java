package com.bankofz.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** CUSTOMER.STATUS - LOGGEDOUT "0" / LOGGEDIN "1" (IBLOGIN1, IBLOGOUT). */
public enum LoginStatus {
    LOGGED_OUT("0"),
    LOGGED_IN("1");

    private final String code;

    LoginStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static LoginStatus fromCode(String code) {
        for (LoginStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown login status: " + code);
    }

    @Converter
    public static class JpaConverter implements AttributeConverter<LoginStatus, String> {
        @Override
        public String convertToDatabaseColumn(LoginStatus value) {
            return value == null ? null : value.code;
        }

        @Override
        public LoginStatus convertToEntityAttribute(String code) {
            return code == null ? null : fromCode(code);
        }
    }
}
