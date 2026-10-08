package com.bankofz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * IBSCUDAT input message. IBSCUDAT overwrites all seven fields, so this is a
 * full replacement (PUT semantics). Sizes follow the CUSTOMER DBD.
 */
public record CustomerUpdateRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @Size(max = 80) String address,
        @Size(max = 25) String city,
        @Size(max = 2) String state,
        @Size(max = 15) String zipCode,
        @Size(max = 12) String phone) {
}
