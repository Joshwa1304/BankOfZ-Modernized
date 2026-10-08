-- Lookup values taken verbatim from the IMS load data
-- (ACCTYPE.data, CUSTTYPE.data, TTYPE.data, TSTATTYP.data).

INSERT INTO account_type (code, description) VALUES
    ('s', 'Savings Account'),
    ('c', 'Checking Account');

INSERT INTO customer_type (code, description) VALUES
    ('c', 'A Regular Customer'),
    ('t', 'A Teller Customer');

INSERT INTO transaction_type (code, description) VALUES
    ('d', 'Deposit'),
    ('t', 'Transfer'),   -- defined in TTYPE but rejected by IBTRAN (see docs/transformation-decisions.md)
    ('w', 'Withdrawal');

INSERT INTO transaction_status_type (code, description) VALUES
    ('c', 'Synchronous Trans'),
    ('q', 'Asynchronous Trans');
