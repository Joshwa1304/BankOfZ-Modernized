package com.bankofz.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bankofz.domain.Customer;

import jakarta.persistence.LockModeType;

/** Replaces GU/GHU/REPL calls against the CUSTOMER database. */
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /** Equivalent of IMS GHU: holds the customer row for the update that follows. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.custId = :custId")
    Optional<Customer> findByIdForUpdate(@Param("custId") Integer custId);
}
