package com.bankofz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bankofz.domain.CustomerAccount;
import com.bankofz.domain.CustomerAccountId;

/** Replaces GHU/GHN loops over the CUSTACCS database. */
public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, CustomerAccountId> {

    List<CustomerAccount> findByIdCustIdOrderByAccNumAsc(Integer custId);

    List<CustomerAccount> findByIdAccIdOrderByIdCustIdAsc(Long accId);

    boolean existsByIdCustIdAndIdAccId(Integer custId, Long accId);
}
