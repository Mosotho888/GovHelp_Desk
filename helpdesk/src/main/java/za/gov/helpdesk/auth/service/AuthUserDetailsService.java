package za.gov.helpdesk.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import za.gov.helpdesk.employee.exception.UserNotFoundException;
import za.gov.helpdesk.employee.model.Employees;
import za.gov.helpdesk.employee.repository.EmployeesRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final EmployeesRepository employeesRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return (UserDetails) employeesRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }
}
