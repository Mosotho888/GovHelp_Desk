package com.loggingsystem.springjwtauth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record RegisterRequest(
        @NotEmpty String firstName,
        @NotEmpty String lastName,
        @NotEmpty @Email String email,
        @NotEmpty String password,
        @NotEmpty String phoneNumber
) {}
