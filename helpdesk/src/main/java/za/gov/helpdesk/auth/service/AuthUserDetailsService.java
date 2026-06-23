package za.gov.helpdesk.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("User not found: " + email));

        return new CustomUserDetails(user);
    }
}
