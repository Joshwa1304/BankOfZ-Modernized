package com.bankofz.domain;

import java.math.BigDecimal;

import com.bankofz.domain.enums.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** IMS ACCOUNT segment. */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @Column(name = "acc_id")
    private Long accId;

    @Convert(converter = AccountType.JpaConverter.class)
    @Column(name = "acc_type", length = 1, nullable = false)
    private AccountType accType;

    /** BALANCE PIC S9(13)V9(2) COMP-3. */
    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    /** LASTTXID - per-account transaction sequence used to build TXID. */
    @Column(name = "last_tx_id", nullable = false)
    private Long lastTxId = 0L;
}
