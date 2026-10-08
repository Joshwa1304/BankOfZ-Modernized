package com.bankofz;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;

// Authentication is JWT-based (SecurityConfig); no default in-memory user is wanted.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BankOfZApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankOfZApplication.class, args);
    }

    /** Replaces FUNCTION CURRENT-DATE; injectable so tests can fix the time. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
