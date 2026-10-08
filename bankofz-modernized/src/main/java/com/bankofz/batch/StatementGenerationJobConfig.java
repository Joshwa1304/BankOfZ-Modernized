package com.bankofz.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import com.bankofz.domain.Account;
import com.bankofz.dto.StatementDto;
import com.bankofz.repository.AccountRepository;
import com.bankofz.service.StatementService;

import lombok.extern.slf4j.Slf4j;

/**
 * BNKSTMT batch job. The DATECARD and SORTCODE DD cards become job parameters:
 *
 * <pre>
 *   period     yyyyMM or yyyy-MM   (DATECARD; blank = current month to date)
 *   sortCode   6 digits            (SORTCODE; blank = bankofz.statement.default-sort-code)
 *   outputDir  directory           (replaces SYSPRINT; one file per account)
 * </pre>
 */
@Slf4j
@Configuration
public class StatementGenerationJobConfig {

    private static final DateTimeFormatter FILE_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    @Bean
    public Job statementGenerationJob(JobRepository jobRepository, Step statementStep) {
        return new JobBuilder("statementGenerationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(statementStep)
                .build();
    }

    @Bean
    public Step statementStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                              Tasklet statementTasklet) {
        return new StepBuilder("statementStep", jobRepository)
                .tasklet(statementTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet statementTasklet(
            StatementService statementService,
            AccountRepository accountRepository,
            @Value("#{jobParameters['period']}") String period,
            @Value("#{jobParameters['sortCode']}") String sortCode,
            @Value("#{jobParameters['outputDir'] ?: '${bankofz.statement.output-dir:statements}'}") String outputDir) {
        return (contribution, chunkContext) -> {
            StatementService.Period statementPeriod = statementService.resolvePeriod(period);
            String resolvedSortCode = statementService.resolveSortCode(sortCode);
            Path dir = Files.createDirectories(Path.of(outputDir));
            log.info("BNKSTMT: sort code {}, period {} to {}, output {}",
                    resolvedSortCode, statementPeriod.from(), statementPeriod.to(), dir.toAbsolutePath());

            int generated = 0;
            for (Account account : accountRepository.findAll(Sort.by("accId"))) {
                StatementDto statement = statementService.generate(account.getAccId(), statementPeriod, resolvedSortCode);
                Path file = dir.resolve("STMT-%s-%d-%s.txt".formatted(resolvedSortCode, account.getAccId(),
                        statementPeriod.from().format(FILE_MONTH)));
                Files.writeString(file, statementService.render(statement));
                contribution.incrementWriteCount(1);
                generated++;
            }
            log.info("BNKSTMT COMPLETED SUCCESSFULLY - TOTAL STATEMENTS GENERATED: {}", generated);
            return RepeatStatus.FINISHED;
        };
    }
}
