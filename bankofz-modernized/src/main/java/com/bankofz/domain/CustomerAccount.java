package com.bankofz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** IMS CUSTACCS segment - links a customer to the accounts they hold. */
@Entity
@Table(name = "customer_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAccount {

    @EmbeddedId
    private CustomerAccountId id;

    /** ACCNUM - account sequence number for the customer. */
    @Column(name = "acc_num", nullable = false)
    private Integer accNum;
}
