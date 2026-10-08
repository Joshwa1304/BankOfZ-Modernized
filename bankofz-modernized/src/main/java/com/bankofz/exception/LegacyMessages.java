package com.bankofz.exception;

/** MSG-OUT literals from the IMS COBOL programs, preserved verbatim for clients. */
public final class LegacyMessages {

    public static final String NO_CUSTOMER = "CUSTOMER DOES NOT EXIST";
    public static final String NO_ACCOUNT = "ACCOUNT DOES NOT EXIST";
    public static final String PASSWORD_INVALID = "PASSWORD INVALID";
    public static final String CUSTOMER_LOGGED_IN = "CUSTOMER ALREADY LOGGED IN";
    public static final String LOGIN_SUCCESSFUL = "LOGIN SUCCESSFUL";
    public static final String LOGOFF_SUCCESSFUL = "LOGOFF SUCCESSFUL";
    public static final String LOGOFF_FAILED = "FAILED UPDATE FOR LOGOFF";
    public static final String INVALID_TRX_TYPE = "INVALID ACCOUNT ACTION. MUST BE 'w' OR 'd'.";

    private LegacyMessages() {
    }
}
