package com.loggingsystem.springjwtauth.auth.jwt;

import com.loggingsystem.springjwtauth.auth.service.AuthUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter is a custom filter that intercepts HTTP requests to validate and authenticate JWT tokens.
 * This filter extracts the token from the HTTP Authorization header, validates it, and then sets the
 * authenticated user in the Spring Security context if the token is valid.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION = "Authorization";

    private final AuthUserDetailsService userDetailsService;

    private final JwtUtil jwtUtil;

    /**
     * Constructor for injecting dependencies into the JwtAuthenticationFilter.
     *
     * @param userDetailsService service to load user details from the database.
     * @param jwtUtil utility class to handle JWT creation and validation.
     */
    public JwtAuthenticationFilter(AuthUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * This method filters incoming HTTP requests by checking the Authorization header for a valid JWT.
     * If a valid token is found, it authenticates the user and sets the authentication context.
     *
     * @param request the HTTP request.
     * @param response the HTTP response.
     * @param filterChain the filter chain that allows further processing of the request.
     * @throws ServletException if the request could not be handled.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromHeader(request);


        if (token != null && jwtUtil.isTokenValid(token)) {
            authenticateUserFromToken(token, request);
        }


        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header of the HTTP request.
     *
     * @param request the HTTP request.
     * @return the JWT token, or null if not found.
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    /**
     * Extracts claims from the provided JWT token, loads the user details,
     * and sets the authentication context in Spring Security.
     *
     * @param token the JWT token to authenticate the user.
     */
    private void authenticateUserFromToken(String token, HttpServletRequest request) {

        String username = jwtUtil.extractUsername(token);


        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        log.info("UserDetails authorities: " + userDetails.getAuthorities());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()

        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

