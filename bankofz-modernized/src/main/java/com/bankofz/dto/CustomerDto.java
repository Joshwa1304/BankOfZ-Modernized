package com.bankofz.dto;

import java.time.LocalDateTime;

/** IBGCUDAT output message. The password is never returned. */
public record CustomerDto(
        Integer custId,
        String firstName,
        String lastName,
        String address,
        String city,
        String state,
        String zipCode,
        String phone,
        String custType,
        LocalDateTime lastLogin) {
}
