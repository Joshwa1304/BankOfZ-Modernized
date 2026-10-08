package com.bankofz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankofz.domain.Account;
import com.bankofz.domain.CustomerAccount;
import com.bankofz.domain.CustomerAccountId;
import com.bankofz.domain.enums.AccountType;
import com.bankofz.exception.CustomerNotFoundException;
import com.bankofz.repository.AccountRepository;
import com.bankofz.repository.CustomerAccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private CustomerAccountRepository customerAccountRepository;
    @Mock
    private AccountRepository accountRepository;

    @Test
    void summaryIsCappedAtConfiguredMaximum() {
        List<CustomerAccount> links = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> new CustomerAccount(new CustomerAccountId(7, 700L + i), i))
                .toList();
        when(customerAccountRepository.findByIdCustIdOrderByAccNumAsc(7)).thenReturn(links);
        when(accountRepository.findById(anyLong())).thenAnswer(inv -> Optional.of(account(inv.getArgument(0))));

        AccountService service = new AccountService(customerAccountRepository, accountRepository, 6);

        assertThat(service.getAccountSummary(7)).hasSize(6)
                .extracting(s -> s.accId())
                .containsExactly(701L, 702L, 703L, 704L, 705L, 706L);
    }

    @Test
    void customerWithoutAccountsIsReportedAsMissing() {
        when(customerAccountRepository.findByIdCustIdOrderByAccNumAsc(5)).thenReturn(List.of());

        AccountService service = new AccountService(customerAccountRepository, accountRepository, 6);

        assertThatThrownBy(() -> service.getAccountSummary(5))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("CUSTOMER DOES NOT EXIST");
        assertThat(service.findAccountSummary(5)).isEmpty();
    }

    private static Account account(Long accId) {
        Account a = new Account();
        a.setAccId(accId);
        a.setAccType(AccountType.SAVINGS);
        a.setBalance(new BigDecimal("10.00"));
        return a;
    }
}
