package com.bankofz.batch;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.function.Function;

import javax.sql.DataSource;

import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Shared plumbing for the IMS load-file migration. The *.data extracts are
 * comma separated with double-quoted text, which is what the LOAD* programs
 * pulled apart with UNSTRING ... DELIMITED BY '","' OR '",' OR ',"' OR ','.
 */
final class LegacyFileSupport {

    /** IMS timestamps such as "2003-07-23 17:56:00.64" (fraction length varies). */
    static final DateTimeFormatter LEGACY_TIMESTAMP = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    private LegacyFileSupport() {
    }

    static <T> FlatFileItemReader<T> reader(String name, Resource resource, FieldSetMapper<T> mapper) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_COMMA);
        tokenizer.setQuoteCharacter('"');
        tokenizer.setStrict(false);
        return new FlatFileItemReaderBuilder<T>()
                .name(name)
                .resource(resource)
                .lineTokenizer(tokenizer)
                .fieldSetMapper(mapper)
                .build();
    }

    static <T> JdbcBatchItemWriter<T> writer(DataSource dataSource, String sql,
                                             Function<T, MapSqlParameterSource> parameters) {
        JdbcBatchItemWriter<T> writer = new JdbcBatchItemWriterBuilder<T>()
                .dataSource(dataSource)
                .sql(sql)
                .itemSqlParameterSourceProvider(parameters::apply)
                // ON CONFLICT DO NOTHING makes re-runs update 0 rows; that is expected.
                .assertUpdates(false)
                .build();
        // Not a Spring bean, so trigger the named-parameter detection ourselves.
        writer.afterPropertiesSet();
        return writer;
    }

    static Timestamp timestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Timestamp.valueOf(LocalDateTime.parse(raw.trim(), LEGACY_TIMESTAMP));
    }
}
