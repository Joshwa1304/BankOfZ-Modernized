package com.bankofz.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bankofz.domain.Customer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Issues and verifies the bearer tokens that replace the IMS LTERM session. */
@Component
public class JwtTokenProvider {

    public static final String CUSTOMER_TYPE_CLAIM = "ctype";

    private final SecretKey key;
    private final Duration ttl;
    private final Clock clock;

    public JwtTokenProvider(@Value("${bankofz.jwt.secret}") String secret,
                            @Value("${bankofz.jwt.expiration-minutes:30}") long expirationMinutes,
                            Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(expirationMinutes);
        this.clock = clock;
    }

    public String createToken(Customer customer) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(customer.getCustId()))
                .claim(CUSTOMER_TYPE_CLAIM, customer.getCustType().getCode())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration getTtl() {
        return ttl;
    }
}
