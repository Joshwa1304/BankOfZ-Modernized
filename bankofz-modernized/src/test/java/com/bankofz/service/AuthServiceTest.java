package com.bankofz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bankofz.domain.Customer;
import com.bankofz.domain.enums.CustomerType;
import com.bankofz.domain.enums.LoginStatus;
import com.bankofz.dto.LoginRequest;
import com.bankofz.dto.LoginResponse;
import com.bankofz.exception.CustomerAlreadyLoggedInException;
import com.bankofz.exception.CustomerNotFoundException;
import com.bankofz.exception.InvalidPasswordException;
import com.bankofz.exception.LegacyMessages;
import com.bankofz.repository.CustomerRepository;
import com.bankofz.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T09:30:00Z");
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;
    private Customer customer;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        authService = new AuthService(customerRepository, ENCODER, jwtTokenProvider, clock);

        customer = new Customer();
        customer.setCustId(1);
        customer.setFirstName("Martha");
        customer.setLastName("Antonelli");
        customer.setCustType(CustomerType.REGULAR);
        customer.setStatus(LoginStatus.LOGGED_OUT);
        customer.setPasswordHash(ENCODER.encode("password"));
    }

    @Test
    void loginSetsStatusAndLastLoginAndReturnsToken() {
        when(customerRepository.findByIdForUpdate(1)).thenReturn(Optional.of(customer));
        when(jwtTokenProvider.createToken(customer)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest(1, "password"));

        assertThat(response.message()).isEqualTo(LegacyMessages.LOGIN_SUCCESSFUL);
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(customer.getStatus()).isEqualTo(LoginStatus.LOGGED_IN);
        assertThat(customer.getLastLogin()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    @Test
    void loginUnknownCustomerFails() {
        when(customerRepository.findByIdForUpdate(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(99, "password")))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("CUSTOMER DOES NOT EXIST");
    }

    @Test
    void loginWithBadPasswordFailsAndLeavesStatusUnchanged() {
        when(customerRepository.findByIdForUpdate(1)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> authService.login(new LoginRequest(1, "wrong")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("PASSWORD INVALID");
        assertThat(customer.getStatus()).isEqualTo(LoginStatus.LOGGED_OUT);
        verify(jwtTokenProvider, never()).createToken(any());
    }

    @Test
    void passwordIsCheckedBeforeLoggedInStatus() {
        customer.setStatus(LoginStatus.LOGGED_IN);
        when(customerRepository.findByIdForUpdate(1)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> authService.login(new LoginRequest(1, "wrong")))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void loginWhileSessionActiveFails() {
        customer.setStatus(LoginStatus.LOGGED_IN);
        customer.setLastLogin(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusMinutes(5));
        when(customerRepository.findByIdForUpdate(1)).thenReturn(Optional.of(customer));
        when(jwtTokenProvider.getTtl()).thenReturn(Duration.ofMinutes(30));

        assertThatThrownBy(() -> authService.login(new LoginRequest(1, "password")))
                .isInstanceOf(CustomerAlreadyLoggedInException.class)
                .hasMessage("CUSTOMER ALREADY LOGGED IN");
    }

    @Test
    void loginAllowedOnceStaleSessionHasExpired() {
        customer.setStatus(LoginStatus.LOGGED_IN);
        customer.setLastLogin(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(2));
        when(customerRepository.findByIdForUpdate(1)).thenReturn(Optional.of(customer));
        when(jwtTokenProvider.getTtl()).thenReturn(Duration.ofMinutes(30));
        when(jwtTokenProvider.createToken(customer)).thenReturn("jwt-token");

        assertThat(authService.login(new LoginRequest(1, "password")).token()).isEqualTo("jwt-token");
    }

    @Test
    void logoutResetsStatus() {
        customer.setStatus(LoginStatus.LOGGED_IN);
        when(customerRepository.findByIdForUpdate(1)).thenReturn(Optional.of(customer));

        assertThat(authService.logout(1).message()).isEqualTo("LOGOFF SUCCESSFUL");
        assertThat(customer.getStatus()).isEqualTo(LoginStatus.LOGGED_OUT);
    }

    @Test
    void logoutUnknownCustomerUsesLegacyMessage() {
        when(customerRepository.findByIdForUpdate(42)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(42))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("FAILED UPDATE FOR LOGOFF");
    }
}
