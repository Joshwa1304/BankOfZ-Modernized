package com.bankofz.dto;

import java.time.LocalDateTime;

public record LoginResponse(
        String message,
        Integer custId,
        String firstName,
        String lastName,
        LocalDateTime lastLogin,
        String token,
        String tokenType) {
}
