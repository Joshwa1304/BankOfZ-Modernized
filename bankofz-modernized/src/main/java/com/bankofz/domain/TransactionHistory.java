package com.bankofz.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bankofz.domain.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** IMS HISTORY segment; also replaces the DB2 copy written through the JNI bridge (InsertHist). */
@Entity
@Table(name = "transaction_history")
@Getter
@Setter
@NoArgsConstructor
public class TransactionHistory {

    @Id
    @Column(name = "tx_id")
    private Long txId;

    @Column(name = "tx_time", nullable = false)
    private LocalDateTime txTime;

    @Convert(converter = TransactionType.JpaConverter.class)
    @Column(name = "trans_type", length = 1, nullable = false)
    private TransactionType transType;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "ref_tx_id", nullable = false)
    private Long refTxId = 0L;

    @Column(name = "acc_id", nullable = false)
    private Long accId;

    /** BALANCE-HIST - balance after posting; null for rows migrated from IMS HISTORY. */
    @Column(name = "balance", precision = 15, scale = 2)
    private BigDecimal balance;

    /** Signed effect on the account balance: deposits are credits, everything else is a debit. */
    public BigDecimal signedAmount() {
        return transType == TransactionType.DEPOSIT ? amount : amount.negate();
    }
}
