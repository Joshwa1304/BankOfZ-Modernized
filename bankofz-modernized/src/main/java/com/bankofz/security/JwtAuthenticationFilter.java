package com.bankofz.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bankofz.domain.Customer;
import com.bankofz.domain.enums.CustomerType;
import com.bankofz.repository.CustomerRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates requests carrying a valid bearer token. The customer must still
 * be logged in (STATUS = '1'), so a token stops working as soon as the customer
 * logs off - the same session semantics as IBLOGIN1 / IBLOGOUT.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_TELLER = "ROLE_TELLER";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final CustomerRepository customerRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomerRepository customerRepository) {
        this.tokenProvider = tokenProvider;
        this.customerRepository = customerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            tokenProvider.parse(header.substring(BEARER_PREFIX.length()))
                    .map(claims -> parseCustomerId(claims.getSubject()))
                    .flatMap(customerRepository::findById)
                    .filter(Customer::isLoggedIn)
                    .ifPresent(this::authenticate);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(Customer customer) {
        String role = customer.getCustType() == CustomerType.TELLER ? ROLE_TELLER : ROLE_CUSTOMER;
        var authentication = new UsernamePasswordAuthenticationToken(
                String.valueOf(customer.getCustId()), null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Integer parseCustomerId(String subject) {
        try {
            return Integer.valueOf(subject);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
