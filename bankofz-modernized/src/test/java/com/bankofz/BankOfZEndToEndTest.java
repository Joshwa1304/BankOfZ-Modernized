package com.bankofz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Boots the whole application against a real (embedded) PostgreSQL 16, runs the
 * Flyway migrations, migrates the real IMS extracts from data/ims with the batch
 * job, then drives the REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BankOfZEndToEndTest {

    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    @Qualifier("imsDataMigrationJob")
    private Job migrationJob;
    @Autowired
    @Qualifier("statementGenerationJob")
    private Job statementJob;

    @BeforeAll
    void migrateImsExtracts() throws Exception {
        JobExecution execution = jobLauncher.run(migrationJob, new JobParametersBuilder()
                .addString("inputDir", "data/ims")
                .addLong("run.id", System.nanoTime())
                .toJobParameters());
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void migrationLoadsEveryImsSegment() {
        // Record counts of the IMS extracts in data/ims.
        assertThat(count("customer")).isEqualTo(100);
        assertThat(count("account")).isEqualTo(265);
        assertThat(count("customer_account")).isEqualTo(265);
        // Migrated rows carry no BALANCE-HIST; rows posted by other tests do.
        assertThat(count("transaction_history where balance is null")).isEqualTo(265);
        assertThat(count("transaction_status")).isEqualTo(265);
        String hash = jdbc.queryForObject("select password_hash from customer where cust_id = 1", String.class);
        assertThat(hash).startsWith("$2a$").isNotEqualTo("password");
    }

    @Test
    void customerSessionAndAccountActivity() throws Exception {
        String token = login(1, "password");

        mvc.perform(get("/customers/1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Antonelli"))
                .andExpect(jsonPath("$.state").value("TX"));

        mvc.perform(get("/customers/1/accounts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accId").value(101))
                .andExpect(jsonPath("$[0].balance").value(8830.00));

        // IBTRAN deposit: TXID = 101 * 10^10 + LASTTXID(1) + 1
        mvc.perform(post("/accounts/101/transactions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trxType\":\"d\",\"amount\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.txId").value(1010000000002L))
                .andExpect(jsonPath("$.balance").value(8930.00))
                .andExpect(jsonPath("$.accounts.length()").value(2));

        mvc.perform(post("/accounts/101/transactions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trxType\":\"W\",\"amount\":30.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.txId").value(1010000000003L))
                .andExpect(jsonPath("$.balance").value(8899.50));

        mvc.perform(post("/accounts/101/transactions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trxType\":\"t\",\"amount\":1.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("INVALID ACCOUNT ACTION. MUST BE 'w' OR 'd'."));

        mvc.perform(get("/accounts/101/transactions").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].txId").value(1010000000003L));

        mvc.perform(get("/accounts/101/statements/" + YearMonth.now())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.totalCredits").value(100.00))
                .andExpect(jsonPath("$.totalDebits").value(30.50))
                .andExpect(jsonPath("$.openingBalance").value(8830.00))
                .andExpect(jsonPath("$.closingBalance").value(8899.50));

        mvc.perform(put("/customers/1").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Martha","lastName":"Antonelli","address":"1 New Street",
                                 "city":"AUSTIN","state":"TX","zipCode":"73301","phone":"914-513-5089"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("AUSTIN"));

        // Ownership: customer 1 cannot see customer 2 or post to account 201.
        mvc.perform(get("/customers/2").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/accounts/201/transactions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trxType\":\"d\",\"amount\":1.00}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("LOGOFF SUCCESSFUL"));

        // STATUS is back to '0', so the old token no longer works and a new login succeeds.
        mvc.perform(get("/customers/1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
        assertThat(login(1, "password")).isNotBlank();
    }

    @Test
    void loginBusinessRules() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"custId\":9999,\"password\":\"password\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("CUSTOMER DOES NOT EXIST"));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"custId\":3,\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("PASSWORD INVALID"));

        login(3, "password");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"custId\":3,\"password\":\"password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("CUSTOMER ALREADY LOGGED IN"));

        mvc.perform(get("/customers/3")).andExpect(status().isUnauthorized());
    }

    @Test
    void statementJobWritesOneFilePerAccount(@TempDir Path outputDir) throws Exception {
        JobExecution execution = jobLauncher.run(statementJob, new JobParametersBuilder()
                .addString("period", "200307")
                .addString("sortCode", "123456")
                .addString("outputDir", outputDir.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        try (Stream<Path> files = Files.list(outputDir)) {
            assertThat(files.count()).isEqualTo(265);
        }
        String statement = Files.readString(outputDir.resolve("STMT-123456-101-200307.txt"));
        assertThat(statement).contains("PERIOD    : 2003-07-01 TO 2003-07-31", "TRANSACTION COUNT:",
                "Thank you for banking with Bank of Z");
    }

    private String login(int custId, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"custId\":" + custId + ",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("LOGIN SUCCESSFUL"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private Integer count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
