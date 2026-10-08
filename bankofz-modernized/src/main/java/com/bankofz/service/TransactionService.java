package com.bankofz.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankofz.domain.Account;
import com.bankofz.domain.TransactionHistory;
import com.bankofz.domain.TransactionIds;
import com.bankofz.domain.enums.TransactionType;
import com.bankofz.dto.TransactionHistoryDto;
import com.bankofz.dto.TransactionRequest;
import com.bankofz.dto.TransactionResponse;
import com.bankofz.exception.AccountNotFoundException;
import com.bankofz.exception.InvalidTransactionTypeException;
import com.bankofz.repository.AccountRepository;
import com.bankofz.repository.TransactionHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * IBTRAN account activity. The IMS HISTORY insert and the DB2 copy written via
 * the JNI bridge (InsertHist) are replaced by a single insert in one
 * {@code @Transactional} unit together with the account update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionHistoryRepository historyRepository;
    private final AccountService accountService;
    private final Clock clock;

    @Transactional
    public TransactionResponse post(Long accId, TransactionRequest request, Integer custId) {
        // IBTRAN validates the action before touching the database.
        TransactionType type = parseTrxType(request.trxType());

        Account account = accountRepository.findByIdForUpdate(accId)
                .orElseThrow(AccountNotFoundException::new);

        // COMPUTE AMOUNT-HIST = FUNCTION NUMVAL(IN-AMOUNT) into V99: extra decimals are truncated.
        BigDecimal amount = request.amount().setScale(2, RoundingMode.DOWN);
        long txId = TransactionIds.next(account.getAccId(), account.getLastTxId());

        BigDecimal newBalance = type == TransactionType.WITHDRAWAL
                ? account.getBalance().subtract(amount)
                : account.getBalance().add(amount);
        account.setBalance(newBalance);
        account.setLastTxId(account.getLastTxId() + 1);

        TransactionHistory history = new TransactionHistory();
        history.setTxId(txId);
        history.setTxTime(LocalDateTime.now(clock));
        history.setTransType(type);
        history.setAmount(amount);
        history.setRefTxId(0L);
        history.setAccId(account.getAccId());
        history.setBalance(newBalance);
        historyRepository.save(history);

        log.info("Posted {} {} to account {} (txid {}), new balance {}",
                type, amount, accId, txId, newBalance);

        return new TransactionResponse(txId, account.getAccId(), type.getCode(), amount, newBalance,
                history.getTxTime(), custId == null ? List.of() : accountService.findAccountSummary(custId));
    }

    @Transactional(readOnly = true)
    public List<TransactionHistoryDto> getHistory(Long accId, int limit) {
        if (!accountRepository.existsById(accId)) {
            throw new AccountNotFoundException();
        }
        return historyRepository.findByAccIdOrderByTxTimeDescTxIdDesc(accId, PageRequest.of(0, limit)).stream()
                .map(TransactionService::toDto)
                .toList();
    }

    /**
     * IF IN-TRXTYPE NOT = 'd' AND NOT = 'w' AND NOT = 'D' AND NOT = 'W' -> INVALIDTRXTYPE.
     * Transfer ('t') exists in TTYPE but is rejected here, exactly as in IBTRAN.
     */
    static TransactionType parseTrxType(String raw) {
        if (raw != null) {
            switch (raw) {
                case "d", "D" -> {
                    return TransactionType.DEPOSIT;
                }
                case "w", "W" -> {
                    return TransactionType.WITHDRAWAL;
                }
                default -> {
                }
            }
        }
        throw new InvalidTransactionTypeException();
    }

    static TransactionHistoryDto toDto(TransactionHistory h) {
        return new TransactionHistoryDto(h.getTxId(), h.getTxTime(), h.getTransType().getCode(),
                h.getAmount(), h.getRefTxId(), h.getBalance());
    }
}
