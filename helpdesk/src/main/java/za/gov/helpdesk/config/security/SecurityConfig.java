package za.gov.helpdesk.config.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import za.gov.helpdesk.auth.jwt.JwtAuthenticationFilter;

/**
 * Core security configuration component defining the application's network security perimeter
 * layer. Configures the primary web filter chains, HTTP firewall rule sets, request routing
 * authentication requirements, custom error response handlers, session policies, and core
 * cryptographic password encoders.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final int PASSWORD_STRENGTH = 12;
    private static final long MAX_AGE_IN_SECONDS = 31536000;

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final HandlerExceptionResolver resolver;

    public SecurityConfig(
            UserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.resolver = resolver;
    }

    /**
     * Constructs and wires the primary {@link SecurityFilterChain} defining request-matching rules
     * and request boundaries. Enforces a completely stateless session management profile, injectors
     * custom response payloads for unauthenticated/unauthorized entry rejections, builds custom
     * HSTS/CSP headers, and establishes filter ordering placement.
     *
     * @param http the security configuration manager wrapper engine to customize
     * @return a constructed and active {@link SecurityFilterChain} mapping
     * @throws Exception if an internal state initialization or runtime filter boundary
     *     configuration error occurs
     */
    @Bean
    public SecurityFilterChain configure(final HttpSecurity http) throws Exception {
        return http.cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/v1/auth/**", "/v1/health")
                                        .permitAll()
                                        .requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/actuator/health",
                                                "/actuator/prometheus")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(
                                                (request, response, authException) -> {
                                                    resolver.resolveException(
                                                            request, response, null, authException);
                                                })
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) -> {
                                                    resolver.resolveException(
                                                            request,
                                                            response,
                                                            null,
                                                            accessDeniedException);
                                                }))
                .authenticationManager(authenticationManager())
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(
                        headers ->
                                headers.httpStrictTransportSecurity(
                                                hsts ->
                                                        hsts.includeSubDomains(true)
                                                                .maxAgeInSeconds(
                                                                        MAX_AGE_IN_SECONDS))
                                        .contentSecurityPolicy(
                                                csp ->
                                                        csp.policyDirectives(
                                                                "default-src 'self';"
                                                                    + " frame-ancestors 'none'"))
                                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                                        .xssProtection(HeadersConfigurer.XXssConfig::disable))
                .build();
    }

    /**
     * Instantiates the primary {@link AuthenticationManager} using a data-access-object based
     * provider module linked to the identity storage user service.
     *
     * @return a centralized {@link AuthenticationManager} engine handling credential lookup
     *     evaluations
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        final DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(provider);
    }

    /**
     * Instantiates the primary secure password encoder leveraging the BCrypt strong-hashing
     * algorithm. Applies an explicit logarithmic work factor to adjust security computational
     * overhead resilience.
     *
     * @return a thread-safe {@link PasswordEncoder} interface bean mapping to BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(PASSWORD_STRENGTH);
    }
}
