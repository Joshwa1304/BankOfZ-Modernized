package com.bankofz.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankofz.domain.Customer;
import com.bankofz.domain.enums.LoginStatus;
import com.bankofz.dto.LoginRequest;
import com.bankofz.dto.LoginResponse;
import com.bankofz.dto.MessageResponse;
import com.bankofz.exception.CustomerAlreadyLoggedInException;
import com.bankofz.exception.CustomerNotFoundException;
import com.bankofz.exception.InvalidPasswordException;
import com.bankofz.exception.LegacyMessages;
import com.bankofz.repository.CustomerRepository;
import com.bankofz.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** IBLOGIN1 / IBLOGIN and IBLOGOUT. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    /**
     * IBLOGIN1 LOGIN paragraph. Checks run in the legacy order: customer exists,
     * password matches, customer not already logged in. On success STATUS is set
     * to '1' and LASTLOGIN to the current timestamp.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByIdForUpdate(request.custId())
                .orElseThrow(CustomerNotFoundException::new);

        if (!passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            throw new InvalidPasswordException();
        }
        if (customer.isLoggedIn() && sessionStillActive(customer)) {
            throw new CustomerAlreadyLoggedInException();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        customer.setLastLogin(now);
        customer.setStatus(LoginStatus.LOGGED_IN);
        log.info("Customer {} logged in", customer.getCustId());

        return new LoginResponse(LegacyMessages.LOGIN_SUCCESSFUL, customer.getCustId(),
                customer.getFirstName(), customer.getLastName(), now,
                jwtTokenProvider.createToken(customer), "Bearer");
    }

    /** IBLOGOUT LOGOUT paragraph: resets STATUS to '0'. */
    @Transactional
    public MessageResponse logout(Integer custId) {
        Customer customer = customerRepository.findByIdForUpdate(custId)
                .orElseThrow(() -> new CustomerNotFoundException(LegacyMessages.LOGOFF_FAILED));
        customer.setStatus(LoginStatus.LOGGED_OUT);
        log.info("Customer {} logged off", custId);
        return new MessageResponse(LegacyMessages.LOGOFF_SUCCESSFUL);
    }

    /**
     * The legacy rule blocks a second login while STATUS = '1'. Because a JWT
     * expires, a customer whose token has expired could otherwise never log in
     * again (logout needs a valid token). A login is therefore only rejected
     * while the previous session is still within the token lifetime.
     */
    private boolean sessionStillActive(Customer customer) {
        LocalDateTime lastLogin = customer.getLastLogin();
        return lastLogin == null
                || lastLogin.plus(jwtTokenProvider.getTtl()).isAfter(LocalDateTime.now(clock));
    }
}
