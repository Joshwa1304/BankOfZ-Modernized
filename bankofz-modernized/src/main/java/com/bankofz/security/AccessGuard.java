package com.bankofz.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.bankofz.repository.CustomerAccountRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ownership checks. Customers may only see their own data and accounts;
 * tellers (CUSTTYPE 't') may act on any customer or account.
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final CustomerAccountRepository customerAccountRepository;

    public Integer currentCustomerId(Authentication authentication) {
        return Integer.valueOf(authentication.getName());
    }

    public boolean isTeller(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> JwtAuthenticationFilter.ROLE_TELLER.equals(a.getAuthority()));
    }

    public void checkCustomer(Authentication authentication, Integer custId) {
        if (!isTeller(authentication) && !currentCustomerId(authentication).equals(custId)) {
            throw new AccessDeniedException("Not permitted to access customer " + custId);
        }
    }

    public void checkAccount(Authentication authentication, Long accId) {
        if (!isTeller(authentication)
                && !customerAccountRepository.existsByIdCustIdAndIdAccId(currentCustomerId(authentication), accId)) {
            throw new AccessDeniedException("Not permitted to access account " + accId);
        }
    }
}
