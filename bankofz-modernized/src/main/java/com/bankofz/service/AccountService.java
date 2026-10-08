package com.bankofz.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankofz.domain.Account;
import com.bankofz.dto.AccountSummaryDto;
import com.bankofz.exception.AccountNotFoundException;
import com.bankofz.exception.CustomerNotFoundException;
import com.bankofz.repository.AccountRepository;
import com.bankofz.repository.CustomerAccountRepository;

/** IBACSUM account summary (also the GET-ACCOUNT-SUMMARY paragraph copied into IBTRAN). */
@Service
public class AccountService {

    private final CustomerAccountRepository customerAccountRepository;
    private final AccountRepository accountRepository;
    private final int summaryMaxSize;

    public AccountService(CustomerAccountRepository customerAccountRepository,
                          AccountRepository accountRepository,
                          @Value("${bankofz.account.summary-max-size:6}") int summaryMaxSize) {
        this.customerAccountRepository = customerAccountRepository;
        this.accountRepository = accountRepository;
        this.summaryMaxSize = summaryMaxSize;
    }

    /**
     * IBACSUM: walk CUSTACCS for the customer and read each ACCOUNT. A customer
     * with no CUSTACCS rows gets "CUSTOMER DOES NOT EXIST", as in the legacy code.
     */
    @Transactional(readOnly = true)
    public List<AccountSummaryDto> getAccountSummary(Integer custId) {
        List<AccountSummaryDto> summary = findAccountSummary(custId);
        if (summary.isEmpty()) {
            throw new CustomerNotFoundException();
        }
        return summary;
    }

    /** Same as {@link #getAccountSummary} but returns an empty list instead of failing. */
    @Transactional(readOnly = true)
    public List<AccountSummaryDto> findAccountSummary(Integer custId) {
        return customerAccountRepository.findByIdCustIdOrderByAccNumAsc(custId).stream()
                .limit(summaryMaxSize)
                .map(link -> accountRepository.findById(link.getId().getAccId())
                        .orElseThrow(AccountNotFoundException::new))
                .map(AccountService::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountSummaryDto getAccount(Long accId) {
        return accountRepository.findById(accId)
                .map(AccountService::toSummary)
                .orElseThrow(AccountNotFoundException::new);
    }

    static AccountSummaryDto toSummary(Account account) {
        return new AccountSummaryDto(account.getAccId(), account.getAccType().getCode(), account.getBalance());
    }
}
