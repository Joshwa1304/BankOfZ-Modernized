package com.bankofz.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankofz.dto.CustomerDto;
import com.bankofz.dto.CustomerUpdateRequest;
import com.bankofz.security.AccessGuard;
import com.bankofz.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** IMS transactions IBGCUDAT (GET) and IBSCUDAT (PUT). */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final AccessGuard accessGuard;

    @GetMapping("/{custId}")
    public CustomerDto getCustomer(@PathVariable Integer custId, Authentication authentication) {
        accessGuard.checkCustomer(authentication, custId);
        return customerService.getCustomer(custId);
    }

    @PutMapping("/{custId}")
    public CustomerDto updateCustomer(@PathVariable Integer custId,
                                      @Valid @RequestBody CustomerUpdateRequest request,
                                      Authentication authentication) {
        accessGuard.checkCustomer(authentication, custId);
        return customerService.updateCustomer(custId, request);
    }
}
