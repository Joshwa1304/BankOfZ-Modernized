package com.bankofz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankofz.domain.Account;
import com.bankofz.domain.TransactionHistory;
import com.bankofz.domain.TransactionIds;
import com.bankofz.domain.enums.AccountType;
import com.bankofz.domain.enums.TransactionType;
import com.bankofz.dto.AccountSummaryDto;
import com.bankofz.dto.TransactionRequest;
import com.bankofz.dto.TransactionResponse;
import com.bankofz.exception.AccountNotFoundException;
import com.bankofz.exception.InvalidTransactionTypeException;
import com.bankofz.repository.AccountRepository;
import com.bankofz.repository.TransactionHistoryRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionHistoryRepository historyRepository;
    @Mock
    private AccountService accountService;

    private TransactionService service;
    private Account account;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
        service = new TransactionService(accountRepository, historyRepository, accountService, clock);

        account = new Account();
        account.setAccId(101L);
        account.setAccType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("8830.00"));
        account.setLastTxId(1L);
    }

    @Test
    void depositAddsToBalanceAndWritesHistory() {
        when(accountRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(account));
        when(accountService.findAccountSummary(1))
                .thenReturn(List.of(new AccountSummaryDto(101L, "c", new BigDecimal("8930.00"))));

        TransactionResponse response = service.post(101L, new TransactionRequest("d", new BigDecimal("100.00")), 1);

        assertThat(response.balance()).isEqualByComparingTo("8930.00");
        assertThat(response.accounts()).hasSize(1);
        assertThat(account.getBalance()).isEqualByComparingTo("8930.00");
        assertThat(account.getLastTxId()).isEqualTo(2L);

        ArgumentCaptor<TransactionHistory> saved = ArgumentCaptor.forClass(TransactionHistory.class);
        verify(historyRepository).save(saved.capture());
        assertThat(saved.getValue().getTransType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(saved.getValue().getBalance()).isEqualByComparingTo("8930.00");
        assertThat(saved.getValue().getRefTxId()).isZero();
    }

    @Test
    void withdrawalSubtractsFromBalance() {
        when(accountRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(account));

        TransactionResponse response = service.post(101L, new TransactionRequest("w", new BigDecimal("30.50")), null);

        assertThat(response.balance()).isEqualByComparingTo("8799.50");
        assertThat(response.trxType()).isEqualTo("w");
    }

    @ParameterizedTest
    @ValueSource(strings = {"D", "W"})
    void upperCaseTypesAreAcceptedLikeIbtran(String type) {
        when(accountRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(account));

        TransactionResponse response = service.post(101L, new TransactionRequest(type, BigDecimal.ONE), null);

        assertThat(response.trxType()).isEqualTo(type.toLowerCase());
    }

    @Test
    void transactionIdFollowsLegacyAlgorithm() {
        account.setLastTxId(41L);
        when(accountRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(account));

        TransactionResponse response = service.post(101L, new TransactionRequest("d", BigDecimal.TEN), null);

        // TXID = ACCID * 10^10 + LASTTXID + 1
        assertThat(response.txId()).isEqualTo(101L * 10_000_000_000L + 42L);
        assertThat(response.txId()).isEqualTo(TransactionIds.next(101L, 41L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", "t", "dw", ""})
    void invalidTypeIsRejectedBeforeAnyDatabaseAccess(String type) {
        assertThatThrownBy(() -> service.post(101L, new TransactionRequest(type, BigDecimal.ONE), 1))
                .isInstanceOf(InvalidTransactionTypeException.class)
                .hasMessage("INVALID ACCOUNT ACTION. MUST BE 'w' OR 'd'.");
        verifyNoInteractions(accountRepository, historyRepository);
    }

    @Test
    void unknownAccountFails() {
        when(accountRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.post(999L, new TransactionRequest("d", BigDecimal.ONE), 1))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("ACCOUNT DOES NOT EXIST");
        verify(historyRepository, never()).save(any());
    }

    @Test
    void extraDecimalsAreTruncatedLikeCobolV99() {
        when(accountRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(account));

        TransactionResponse response = service.post(101L, new TransactionRequest("d", new BigDecimal("1.239")), null);

        assertThat(response.amount()).isEqualByComparingTo("1.23");
    }
}
