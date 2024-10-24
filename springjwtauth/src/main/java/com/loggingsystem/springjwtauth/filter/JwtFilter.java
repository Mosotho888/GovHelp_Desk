package com.loggingsystem.springjwtauth.filter;

import com.loggingsystem.springjwtauth.jwtUtil.JwtHelper;
import com.loggingsystem.springjwtauth.service.EmployeeUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;


@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION = "Authorization";

    private final EmployeeUserDetailsService userDetailsService;

    private final JwtHelper jwtHelper;

    public JwtFilter(EmployeeUserDetailsService userDetailsService, JwtHelper jwtHelper) {
        this.userDetailsService = userDetailsService;
        this.jwtHelper = jwtHelper;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("Inside JWT filter");
        try {
            final String authorizationHeader = request.getHeader("Authorization");

            System.out.println("Print Auth header: " + authorizationHeader);
            String jwt = null;
            String useremail = null;
            if (Objects.nonNull(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                System.out.println("JWT Tokwn ONLY: " + jwt);
                useremail = jwtHelper.extractUseremail(jwt);
            }

            System.out.println("Security Context: " + SecurityContextHolder.getContext().getAuthentication());
            if (Objects.nonNull(useremail) && SecurityContextHolder.getContext().getAuthentication() == null) {
                System.out.println("Context username:" + useremail);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(useremail);
                System.out.println("Context user details: " + userDetails);
                boolean isTokenValidated = jwtHelper.validateToken(jwt, userDetails);
                System.out.println("Is token validated: " + isTokenValidated);

                if (isTokenValidated) {
                    System.out.println("UerDetails authorities: " + userDetails.getAuthorities());
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            } else {
                throw new BadCredentialsException("Bearer token not set correctly");
            }
        } catch (ExpiredJwtException jwtException) {
            request.setAttribute("exception", jwtException);
        } catch (BadCredentialsException | UnsupportedJwtException | MalformedJwtException e) {
            log.error("Filter exception: {}", e.getMessage());
            request.setAttribute("exception", e);
        }

        filterChain.doFilter(request, response);

    }
}

