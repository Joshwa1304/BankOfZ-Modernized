package com.bankofz.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankofz.domain.Customer;
import com.bankofz.dto.CustomerDto;
import com.bankofz.dto.CustomerUpdateRequest;
import com.bankofz.exception.CustomerNotFoundException;
import com.bankofz.mapper.CustomerMapper;
import com.bankofz.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

/** IBGCUDAT (get customer data) and IBSCUDAT (set customer data). */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public CustomerDto getCustomer(Integer custId) {
        return customerRepository.findById(custId)
                .map(customerMapper::toDto)
                .orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional
    public CustomerDto updateCustomer(Integer custId, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findByIdForUpdate(custId)
                .orElseThrow(CustomerNotFoundException::new);
        customerMapper.updateCustomer(request, customer);
        return customerMapper.toDto(customer);
    }
}
