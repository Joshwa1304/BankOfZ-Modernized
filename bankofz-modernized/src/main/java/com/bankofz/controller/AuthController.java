package com.bankofz.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bankofz.dto.LoginRequest;
import com.bankofz.dto.LoginResponse;
import com.bankofz.dto.MessageResponse;
import com.bankofz.security.AccessGuard;
import com.bankofz.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** IMS transactions IBLOGIN / IBLOGOUT. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccessGuard accessGuard;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(Authentication authentication) {
        return authService.logout(accessGuard.currentCustomerId(authentication));
    }
}
