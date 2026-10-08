package com.bankofz.domain;

import java.time.LocalDateTime;

import com.bankofz.domain.enums.CustomerType;
import com.bankofz.domain.enums.LoginStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** IMS CUSTOMER segment. */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @Column(name = "cust_id")
    private Integer custId;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "address", length = 80)
    private String address;

    @Column(name = "city", length = 25)
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "zip_code", length = 15)
    private String zipCode;

    @Column(name = "phone", length = 12)
    private String phone;

    @Convert(converter = LoginStatus.JpaConverter.class)
    @Column(name = "status", length = 1, nullable = false)
    private LoginStatus status = LoginStatus.LOGGED_OUT;

    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    @Convert(converter = CustomerType.JpaConverter.class)
    @Column(name = "cust_type", length = 1, nullable = false)
    private CustomerType custType;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    public boolean isLoggedIn() {
        return status == LoginStatus.LOGGED_IN;
    }
}
