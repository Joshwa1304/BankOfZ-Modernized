package com.bankofz.batch;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Migrates the IMS database extracts into PostgreSQL. One step per legacy
 * load program, run in foreign-key order:
 *
 * <pre>
 *   LOADCUST -> loadCustomerStep            (CUSTOMER.data)
 *   LOADACCT -> loadAccountStep             (ACCOUNT.data)
 *   LOADCUSA -> loadCustomerAccountStep     (CUSTACCS.data)
 *   LOADHIST -> loadHistoryStep             (HISTORY.data)
 *   LOADTSTA -> loadTransactionStatusStep   (TSTAT.data)
 * </pre>
 *
 * Job parameter {@code inputDir} (default {@code data/ims}) points at the extracts.
 * Inserts use ON CONFLICT DO NOTHING, so the job can be re-run safely.
 */
@Configuration
public class ImsDataMigrationJobConfig {

    private static final int CHUNK_SIZE = 100;
    private static final String INPUT_DIR = "#{jobParameters['inputDir'] ?: 'data/ims'}";

    // ---- Record layouts, field order as in each program's UNSTRING ... INTO list ----

    record CustomerRecord(Integer custId, String lastName, String firstName, String address, String city,
                          String state, String zipCode, String phone, String status, String password,
                          String custType, Timestamp lastLogin) {

        CustomerRecord withPassword(String newPassword) {
            return new CustomerRecord(custId, lastName, firstName, address, city, state, zipCode, phone,
                    status, newPassword, custType, lastLogin);
        }
    }

    record AccountRecord(Long accId, String accType, BigDecimal balance, Long lastTxId) {
    }

    record CustomerAccountRecord(Long accId, Integer custId, Integer accNum) {
    }

    record HistoryRecord(Long txId, Timestamp txTime, String transType, BigDecimal amount, Long refTxId,
                         Long accId) {
    }

    record TransactionStatusRecord(Long txId, String status, Timestamp startTime, Timestamp stopTime,
                                   Integer custId) {
    }

    @Bean
    public Job imsDataMigrationJob(JobRepository jobRepository,
                                   Step loadCustomerStep,
                                   Step loadAccountStep,
                                   Step loadCustomerAccountStep,
                                   Step loadHistoryStep,
                                   Step loadTransactionStatusStep) {
        return new JobBuilder("imsDataMigrationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(loadCustomerStep)
                .next(loadAccountStep)
                .next(loadCustomerAccountStep)
                .next(loadHistoryStep)
                .next(loadTransactionStatusStep)
                .build();
    }

    // ---- LOADCUST ----

    @Bean
    @StepScope
    public FlatFileItemReader<CustomerRecord> customerReader(@Value(INPUT_DIR) String inputDir) {
        return LegacyFileSupport.reader("customerReader", new FileSystemResource(Path.of(inputDir, "CUSTOMER.data")),
                fs -> new CustomerRecord(fs.readInt(0), fs.readString(1), fs.readString(2), fs.readString(3),
                        fs.readString(4), fs.readString(5), fs.readString(6), fs.readString(7),
                        fs.readString(8), fs.readString(9), fs.readString(10),
                        LegacyFileSupport.timestamp(fs.readString(11))));
    }

    @Bean
    public Step loadCustomerStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                 FlatFileItemReader<CustomerRecord> customerReader,
                                 PasswordEncoder passwordEncoder, DataSource dataSource) {
        return new StepBuilder("loadCustomerStep", jobRepository)
                .<CustomerRecord, CustomerRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerReader)
                // Plaintext IMS passwords are hashed on the way in; they are never stored in clear.
                .processor(r -> r.withPassword(passwordEncoder.encode(r.password())))
                .writer(LegacyFileSupport.<CustomerRecord>writer(dataSource, """
                        INSERT INTO customer (cust_id, last_name, first_name, address, city, state, zip_code,
                                              phone, status, password_hash, cust_type, last_login)
                        VALUES (:custId, :lastName, :firstName, :address, :city, :state, :zipCode,
                                :phone, :status, :passwordHash, :custType, :lastLogin)
                        ON CONFLICT (cust_id) DO NOTHING""",
                        r -> new MapSqlParameterSource()
                                .addValue("custId", r.custId())
                                .addValue("lastName", r.lastName())
                                .addValue("firstName", r.firstName())
                                .addValue("address", r.address())
                                .addValue("city", r.city())
                                .addValue("state", r.state())
                                .addValue("zipCode", r.zipCode())
                                .addValue("phone", r.phone())
                                .addValue("status", r.status())
                                .addValue("passwordHash", r.password())
                                .addValue("custType", r.custType())
                                .addValue("lastLogin", r.lastLogin())))
                .build();
    }

    // ---- LOADACCT ----

    @Bean
    @StepScope
    public FlatFileItemReader<AccountRecord> accountReader(@Value(INPUT_DIR) String inputDir) {
        return LegacyFileSupport.reader("accountReader", new FileSystemResource(Path.of(inputDir, "ACCOUNT.data")),
                fs -> new AccountRecord(fs.readLong(0), fs.readString(1), fs.readBigDecimal(2), fs.readLong(3)));
    }

    @Bean
    public Step loadAccountStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                FlatFileItemReader<AccountRecord> accountReader, DataSource dataSource) {
        return new StepBuilder("loadAccountStep", jobRepository)
                .<AccountRecord, AccountRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(accountReader)
                .writer(LegacyFileSupport.<AccountRecord>writer(dataSource, """
                        INSERT INTO account (acc_id, acc_type, balance, last_tx_id)
                        VALUES (:accId, :accType, :balance, :lastTxId)
                        ON CONFLICT (acc_id) DO NOTHING""",
                        r -> new MapSqlParameterSource()
                                .addValue("accId", r.accId())
                                .addValue("accType", r.accType())
                                .addValue("balance", r.balance())
                                .addValue("lastTxId", r.lastTxId())))
                .build();
    }

    // ---- LOADCUSA ----

    @Bean
    @StepScope
    public FlatFileItemReader<CustomerAccountRecord> customerAccountReader(@Value(INPUT_DIR) String inputDir) {
        return LegacyFileSupport.reader("customerAccountReader",
                new FileSystemResource(Path.of(inputDir, "CUSTACCS.data")),
                fs -> new CustomerAccountRecord(fs.readLong(0), fs.readInt(1), fs.readInt(2)));
    }

    @Bean
    public Step loadCustomerAccountStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                        FlatFileItemReader<CustomerAccountRecord> customerAccountReader,
                                        DataSource dataSource) {
        return new StepBuilder("loadCustomerAccountStep", jobRepository)
                .<CustomerAccountRecord, CustomerAccountRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerAccountReader)
                .writer(LegacyFileSupport.<CustomerAccountRecord>writer(dataSource, """
                        INSERT INTO customer_account (cust_id, acc_id, acc_num)
                        VALUES (:custId, :accId, :accNum)
                        ON CONFLICT (cust_id, acc_id) DO NOTHING""",
                        r -> new MapSqlParameterSource()
                                .addValue("custId", r.custId())
                                .addValue("accId", r.accId())
                                .addValue("accNum", r.accNum())))
                .build();
    }

    // ---- LOADHIST ----

    @Bean
    @StepScope
    public FlatFileItemReader<HistoryRecord> historyReader(@Value(INPUT_DIR) String inputDir) {
        return LegacyFileSupport.reader("historyReader", new FileSystemResource(Path.of(inputDir, "HISTORY.data")),
                fs -> new HistoryRecord(fs.readLong(0), LegacyFileSupport.timestamp(fs.readString(1)),
                        fs.readString(2), fs.readBigDecimal(3), fs.readLong(4), fs.readLong(5)));
    }

    @Bean
    public Step loadHistoryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                FlatFileItemReader<HistoryRecord> historyReader, DataSource dataSource) {
        return new StepBuilder("loadHistoryStep", jobRepository)
                .<HistoryRecord, HistoryRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(historyReader)
                .writer(LegacyFileSupport.<HistoryRecord>writer(dataSource, """
                        INSERT INTO transaction_history (tx_id, tx_time, trans_type, amount, ref_tx_id, acc_id)
                        VALUES (:txId, :txTime, :transType, :amount, :refTxId, :accId)
                        ON CONFLICT (tx_id) DO NOTHING""",
                        r -> new MapSqlParameterSource()
                                .addValue("txId", r.txId())
                                .addValue("txTime", r.txTime())
                                .addValue("transType", r.transType())
                                .addValue("amount", r.amount())
                                .addValue("refTxId", r.refTxId())
                                .addValue("accId", r.accId())))
                .build();
    }

    // ---- LOADTSTA ----

    @Bean
    @StepScope
    public FlatFileItemReader<TransactionStatusRecord> transactionStatusReader(@Value(INPUT_DIR) String inputDir) {
        return LegacyFileSupport.reader("transactionStatusReader",
                new FileSystemResource(Path.of(inputDir, "TSTAT.data")),
                fs -> new TransactionStatusRecord(fs.readLong(0), fs.readString(1),
                        LegacyFileSupport.timestamp(fs.readString(2)), LegacyFileSupport.timestamp(fs.readString(3)),
                        fs.readInt(4)));
    }

    @Bean
    public Step loadTransactionStatusStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                          FlatFileItemReader<TransactionStatusRecord> transactionStatusReader,
                                          DataSource dataSource) {
        return new StepBuilder("loadTransactionStatusStep", jobRepository)
                .<TransactionStatusRecord, TransactionStatusRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(transactionStatusReader)
                .writer(LegacyFileSupport.<TransactionStatusRecord>writer(dataSource, """
                        INSERT INTO transaction_status (tx_id, status, start_time, stop_time, cust_id)
                        VALUES (:txId, :status, :startTime, :stopTime, :custId)
                        ON CONFLICT (tx_id) DO NOTHING""",
                        r -> new MapSqlParameterSource()
                                .addValue("txId", r.txId())
                                .addValue("status", r.status())
                                .addValue("startTime", r.startTime())
                                .addValue("stopTime", r.stopTime())
                                .addValue("custId", r.custId())))
                .build();
    }
}
