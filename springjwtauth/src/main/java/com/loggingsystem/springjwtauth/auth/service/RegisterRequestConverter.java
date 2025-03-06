package com.loggingsystem.springjwtauth.auth.service;

import com.loggingsystem.springjwtauth.auth.dto.RegisterRequest;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Component
public class RegisterRequestConverter implements Converter<RegisterRequest, Employees> {
    private final PasswordEncoder passwordEncoder;

    public RegisterRequestConverter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public @NotNull Employees convert(RegisterRequest registerRequest) {

        String encodedPassword = passwordEncoder.encode(registerRequest.password());

        Employees employee = new Employees();
        BeanUtils.copyProperties(registerRequest, employee);

        employee.setPassword(encodedPassword);
        employee.setRole("USER");
        employee.setCreatedAt(LocalDateTime.now());

        return employee;
    }
}
