package com.bankofz.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankofz.domain.Account;
import com.bankofz.domain.TransactionHistory;
import com.bankofz.domain.enums.TransactionType;
import com.bankofz.dto.StatementDto;
import com.bankofz.dto.TransactionHistoryDto;
import com.bankofz.exception.AccountNotFoundException;
import com.bankofz.exception.InvalidStatementPeriodException;
import com.bankofz.repository.AccountRepository;
import com.bankofz.repository.CustomerAccountRepository;
import com.bankofz.repository.CustomerRepository;
import com.bankofz.repository.TransactionHistoryRepository;

/** BNKSTMT (PL/I) monthly statement generation. */
@Service
public class StatementService {

    private static final DateTimeFormatter DATECARD = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int WIDTH = 72;

    /** Statement period: from the 1st of the month to its last day (or to today for month-to-date). */
    public record Period(LocalDate from, LocalDate to) {

        /** Fixes the BNKSTMT leap-year bug: February always got LAST_DAY = '28'. */
        public static Period ofMonth(YearMonth month) {
            return new Period(month.atDay(1), month.atDay(month.lengthOfMonth()));
        }

        /** BNKSTMT behaviour when no DATECARD is supplied: 1st of the current month to today. */
        public static Period monthToDate(LocalDate today) {
            return new Period(today.withDayOfMonth(1), today);
        }
    }

    private final AccountRepository accountRepository;
    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionHistoryRepository historyRepository;
    private final Clock clock;
    private final String defaultSortCode;

    public StatementService(AccountRepository accountRepository,
                            CustomerAccountRepository customerAccountRepository,
                            CustomerRepository customerRepository,
                            TransactionHistoryRepository historyRepository,
                            Clock clock,
                            @Value("${bankofz.statement.default-sort-code:123456}") String defaultSortCode) {
        this.accountRepository = accountRepository;
        this.customerAccountRepository = customerAccountRepository;
        this.customerRepository = customerRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
        this.defaultSortCode = defaultSortCode;
    }

    /** Accepts the DATECARD format (yyyyMM) or ISO (yyyy-MM); blank means month-to-date. */
    public Period resolvePeriod(String period) {
        if (period == null || period.isBlank()) {
            return Period.monthToDate(LocalDate.now(clock));
        }
        String value = period.trim();
        try {
            return Period.ofMonth(value.contains("-") ? YearMonth.parse(value) : YearMonth.parse(value, DATECARD));
        } catch (DateTimeParseException e) {
            throw new InvalidStatementPeriodException(period);
        }
    }

    public String resolveSortCode(String sortCode) {
        return sortCode == null || sortCode.isBlank() ? defaultSortCode : sortCode.trim();
    }

    @Transactional(readOnly = true)
    public StatementDto generate(Long accId, Period period, String sortCode) {
        Account account = accountRepository.findById(accId).orElseThrow(AccountNotFoundException::new);

        String customerName = customerAccountRepository.findByIdAccIdOrderByIdCustIdAsc(accId).stream()
                .findFirst()
                .flatMap(link -> customerRepository.findById(link.getId().getCustId()))
                .map(c -> c.getFirstName() + " " + c.getLastName())
                .orElse("");

        LocalDateTime periodEnd = period.to().plusDays(1).atStartOfDay();
        List<TransactionHistory> sinceStart = historyRepository
                .findByAccIdAndTxTimeGreaterThanEqualOrderByTxTimeAscTxIdAsc(accId, period.from().atStartOfDay());
        List<TransactionHistory> inPeriod = sinceStart.stream()
                .filter(t -> t.getTxTime().isBefore(periodEnd))
                .toList();

        BigDecimal credits = sum(inPeriod.stream()
                .filter(t -> t.getTransType() == TransactionType.DEPOSIT).toList());
        BigDecimal debits = sum(inPeriod.stream()
                .filter(t -> t.getTransType() != TransactionType.DEPOSIT).toList());

        // BNKSTMT derived the opening balance from the current balance and the period's
        // movements only, which is wrong for any past month. Rolling back every movement
        // since the period start gives the true opening balance.
        BigDecimal movementsSinceStart = sinceStart.stream()
                .map(TransactionHistory::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal opening = account.getBalance().subtract(movementsSinceStart);
        BigDecimal closing = opening.add(credits).subtract(debits);

        return new StatementDto(resolveSortCode(sortCode), accId, account.getAccType().getCode(), customerName,
                period.from(), period.to(),
                inPeriod.stream().map(TransactionService::toDto).toList(),
                opening, credits, debits, closing, account.getBalance());
    }

    /** Text layout modelled on the BNKSTMT SYSPRINT report. */
    public String render(StatementDto s) {
        StringBuilder out = new StringBuilder();
        line(out, center("BANK OF Z - MONTHLY ACCOUNT STATEMENT"));
        line(out, "=".repeat(WIDTH));
        line(out, "SORT CODE : " + s.sortCode());
        line(out, "ACCOUNT   : " + s.accId() + " (" + s.accType() + ")");
        line(out, "CUSTOMER  : " + s.customerName());
        line(out, "PERIOD    : " + s.periodFrom() + " TO " + s.periodTo());
        line(out, "");
        line(out, "TRANSACTION HISTORY:");
        line(out, "");
        line(out, String.format("%-10s %-8s %-4s %-15s %18s", "DATE", "TIME", "TYPE", "REFERENCE", "AMOUNT"));
        line(out, "-".repeat(WIDTH));
        if (s.transactions().isEmpty()) {
            line(out, "  NO TRANSACTIONS FOR THIS PERIOD");
        }
        for (TransactionHistoryDto t : s.transactions()) {
            BigDecimal signed = TransactionType.DEPOSIT.getCode().equals(t.trxType()) ? t.amount() : t.amount().negate();
            line(out, String.format("%-10s %-8s %-4s %-15d %18s", t.txTime().toLocalDate(),
                    t.txTime().format(TIME), t.trxType().toUpperCase(), t.txId(), signed.toPlainString()));
        }
        line(out, "");
        line(out, "================================");
        line(out, "STATEMENT SUMMARY:");
        line(out, "================================");
        line(out, "");
        line(out, money("OPENING BALANCE:", s.openingBalance()));
        line(out, money("TOTAL CREDITS:", s.totalCredits()));
        line(out, money("TOTAL DEBITS:", s.totalDebits()));
        line(out, money("CLOSING BALANCE:", s.closingBalance()));
        line(out, "");
        line(out, money("AVAILABLE BALANCE:", s.availableBalance()));
        line(out, String.format("  %-22s %5d", "TRANSACTION COUNT:", s.transactionCount()));
        line(out, "");
        line(out, center("*** END OF STATEMENT ***"));
        line(out, "");
        line(out, center("Thank you for banking with Bank of Z"));
        return out.toString();
    }

    private static BigDecimal sum(List<TransactionHistory> transactions) {
        return transactions.stream().map(TransactionHistory::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String money(String label, BigDecimal amount) {
        return String.format("  %-22s $%15s", label, amount.toPlainString());
    }

    private static String center(String text) {
        return " ".repeat(Math.max(0, (WIDTH - text.length()) / 2)) + text;
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append(System.lineSeparator());
    }
}
