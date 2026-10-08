package com.bankofz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankofz.domain.Account;
import com.bankofz.domain.TransactionHistory;
import com.bankofz.domain.enums.AccountType;
import com.bankofz.domain.enums.TransactionType;
import com.bankofz.dto.StatementDto;
import com.bankofz.exception.InvalidStatementPeriodException;
import com.bankofz.repository.AccountRepository;
import com.bankofz.repository.CustomerAccountRepository;
import com.bankofz.repository.CustomerRepository;
import com.bankofz.repository.TransactionHistoryRepository;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerAccountRepository customerAccountRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private TransactionHistoryRepository historyRepository;

    private StatementService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC);
        service = new StatementService(accountRepository, customerAccountRepository, customerRepository,
                historyRepository, clock, "123456");
    }

    @ParameterizedTest
    @CsvSource({
            "2024-02, 2024-02-29",   // leap year: BNKSTMT wrongly used 28
            "2026-02, 2026-02-28",
            "2000-02, 2000-02-29",   // divisible by 400
            "1900-02, 1900-02-28",   // divisible by 100, not 400
            "2026-04, 2026-04-30",
            "2026-12, 2026-12-31"})
    void periodEndsOnTheRealLastDayOfMonth(String month, LocalDate expectedEnd) {
        assertThat(StatementService.Period.ofMonth(YearMonth.parse(month)).to()).isEqualTo(expectedEnd);
    }

    @Test
    void periodAcceptsDatecardAndIsoFormatsAndDefaultsToMonthToDate() {
        assertThat(service.resolvePeriod("202402").to()).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(service.resolvePeriod("2024-02").from()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(service.resolvePeriod(null))
                .isEqualTo(new StatementService.Period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7)));
        assertThatThrownBy(() -> service.resolvePeriod("2024/02"))
                .isInstanceOf(InvalidStatementPeriodException.class);
    }

    @Test
    void sortCodeDefaultsLikeBnkstmt() {
        assertThat(service.resolveSortCode(" ")).isEqualTo("123456");
        assertThat(service.resolveSortCode("654321")).isEqualTo("654321");
    }

    @Test
    void openingBalanceRollsBackEveryMovementSinceThePeriodStarted() {
        Account account = new Account();
        account.setAccId(101L);
        account.setAccType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("1000.00"));
        when(accountRepository.findById(101L)).thenReturn(Optional.of(account));
        when(customerAccountRepository.findByIdAccIdOrderByIdCustIdAsc(101L)).thenReturn(List.of());
        when(historyRepository.findByAccIdAndTxTimeGreaterThanEqualOrderByTxTimeAscTxIdAsc(eq(101L), any()))
                .thenReturn(List.of(
                        tx(1, "2024-02-10T09:00", TransactionType.DEPOSIT, "200.00"),
                        tx(2, "2024-02-29T23:59", TransactionType.WITHDRAWAL, "50.00"),
                        // after the period: must not appear on the statement but affects the opening balance
                        tx(3, "2024-03-02T08:00", TransactionType.DEPOSIT, "100.00")));

        StatementDto s = service.generate(101L, StatementService.Period.ofMonth(YearMonth.of(2024, 2)), null);

        assertThat(s.transactions()).extracting(t -> t.txId()).containsExactly(1L, 2L);
        assertThat(s.totalCredits()).isEqualByComparingTo("200.00");
        assertThat(s.totalDebits()).isEqualByComparingTo("50.00");
        // 1000 - (200 - 50 + 100) = 750 ; closing = 750 + 200 - 50 = 900 (= balance before the March deposit)
        assertThat(s.openingBalance()).isEqualByComparingTo("750.00");
        assertThat(s.closingBalance()).isEqualByComparingTo("900.00");
        assertThat(s.availableBalance()).isEqualByComparingTo("1000.00");
        assertThat(service.render(s)).contains("STATEMENT SUMMARY:", "SORT CODE : 123456", "TRANSACTION COUNT:");
    }

    private static TransactionHistory tx(long id, String time, TransactionType type, String amount) {
        TransactionHistory h = new TransactionHistory();
        h.setTxId(id);
        h.setTxTime(LocalDateTime.parse(time));
        h.setTransType(type);
        h.setAmount(new BigDecimal(amount));
        h.setAccId(101L);
        return h;
    }
}
