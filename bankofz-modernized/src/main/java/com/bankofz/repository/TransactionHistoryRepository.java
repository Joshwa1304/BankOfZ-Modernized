package com.bankofz.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bankofz.domain.TransactionHistory;

/** Replaces ISRT into the HISTORY database and the DB2 PROCTRAN cursor used by BNKSTMT. */
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    List<TransactionHistory> findByAccIdAndTxTimeGreaterThanEqualOrderByTxTimeAscTxIdAsc(
            Long accId, LocalDateTime from);

    List<TransactionHistory> findByAccIdOrderByTxTimeDescTxIdDesc(Long accId, Pageable pageable);
}
