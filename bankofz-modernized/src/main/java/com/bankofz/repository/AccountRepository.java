package com.bankofz.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bankofz.domain.Account;

import jakarta.persistence.LockModeType;

/** Replaces GU/GHU/REPL calls against the ACCOUNT database. */
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Equivalent of IMS GHU (get hold unique): locks the row until the
     * transaction commits so concurrent postings cannot lose updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accId = :accId")
    Optional<Account> findByIdForUpdate(@Param("accId") Long accId);
}
