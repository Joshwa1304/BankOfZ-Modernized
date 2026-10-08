-- Bank of Z: IMS DB segments mapped to PostgreSQL.
-- Column sizes follow the IMS DBD FIELD definitions (src/base/ims/DBD/*.asm).

-- ACCTYPE / CUSTTYPE / TTYPE / TSTATTYP lookup databases
CREATE TABLE account_type (
    code        VARCHAR(1)  PRIMARY KEY,
    description VARCHAR(40) NOT NULL
);

CREATE TABLE customer_type (
    code        VARCHAR(1)  PRIMARY KEY,
    description VARCHAR(40) NOT NULL
);

CREATE TABLE transaction_type (
    code        VARCHAR(1)  PRIMARY KEY,
    description VARCHAR(40) NOT NULL
);

CREATE TABLE transaction_status_type (
    code        VARCHAR(1)  PRIMARY KEY,
    description VARCHAR(40) NOT NULL
);

-- CUSTOMER segment
CREATE TABLE customer (
    cust_id       INTEGER      PRIMARY KEY,                    -- CUSTID    4 bytes
    last_name     VARCHAR(50)  NOT NULL,                       -- LASTNAME  50
    first_name    VARCHAR(50)  NOT NULL,                       -- FIRSTNM   50
    address       VARCHAR(80),                                 -- ADDRESS   80
    city          VARCHAR(25),                                 -- CITY      25
    state         VARCHAR(2),                                  -- STATE      2
    zip_code      VARCHAR(15),                                 -- ZIPCODE   15
    phone         VARCHAR(12),                                 -- PHONE     12
    status        VARCHAR(1)   NOT NULL DEFAULT '0',           -- STATUS: '0' logged out, '1' logged in
    password_hash VARCHAR(100) NOT NULL,                       -- PASSWORD: was 16-byte plaintext, now BCrypt
    cust_type     VARCHAR(1)   NOT NULL REFERENCES customer_type (code), -- CUSTTYPE
    last_login    TIMESTAMP                                    -- LASTLOGI  23-byte timestamp text
);

-- ACCOUNT segment
CREATE TABLE account (
    acc_id     BIGINT         PRIMARY KEY,                        -- ACCID    S9(18) COMP-5
    acc_type   VARCHAR(1)     NOT NULL REFERENCES account_type (code), -- ACCTYPE
    balance    NUMERIC(15, 2) NOT NULL DEFAULT 0,                 -- BALANCE  S9(13)V99 COMP-3
    last_tx_id BIGINT         NOT NULL DEFAULT 0                  -- LASTTXID S9(18) COMP-5
);

-- CUSTACCS segment (customer <-> account cross reference)
CREATE TABLE customer_account (
    cust_id INTEGER NOT NULL REFERENCES customer (cust_id),
    acc_id  BIGINT  NOT NULL REFERENCES account (acc_id),
    acc_num INTEGER NOT NULL,                                     -- ACCNUM: account sequence for the customer
    PRIMARY KEY (cust_id, acc_id)
);

-- HISTORY segment (+ the DB2 copy previously written through the JNI bridge)
CREATE TABLE transaction_history (
    tx_id      BIGINT         PRIMARY KEY,                        -- TXID = ACCID * 10^10 + LASTTXID + 1
    tx_time    TIMESTAMP      NOT NULL,                           -- TIME
    trans_type VARCHAR(1)     NOT NULL REFERENCES transaction_type (code), -- TRANSTYP
    amount     NUMERIC(15, 2) NOT NULL,                           -- AMOUNT   S9(13)V99 COMP-3
    ref_tx_id  BIGINT         NOT NULL DEFAULT 0,                 -- REFTXID
    acc_id     BIGINT         NOT NULL REFERENCES account (acc_id), -- ACCID
    balance    NUMERIC(15, 2)                                     -- BALANCE-HIST (null for migrated rows)
);

CREATE INDEX ix_transaction_history_acc_time ON transaction_history (acc_id, tx_time);

-- TSTAT segment
CREATE TABLE transaction_status (
    tx_id      BIGINT     PRIMARY KEY REFERENCES transaction_history (tx_id),
    status     VARCHAR(1) NOT NULL REFERENCES transaction_status_type (code),
    start_time TIMESTAMP  NOT NULL,
    stop_time  TIMESTAMP,
    cust_id    INTEGER    NOT NULL REFERENCES customer (cust_id)
);
