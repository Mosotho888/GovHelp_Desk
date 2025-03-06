package com.loggingsystem.springjwtauth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public record LoginRequest(
        @NotEmpty @Email String userEmail,
        @NotEmpty String password
) {}
