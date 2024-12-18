package com.loggingsystem.springjwtauth.filter;

import com.loggingsystem.springjwtauth.jwtUtil.JwtCreator;
import com.loggingsystem.springjwtauth.service.EmployeeUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION = "Authorization";

    private final EmployeeUserDetailsService userDetailsService;

    private final JwtCreator jwtCreator;

    public JwtFilter(EmployeeUserDetailsService userDetailsService, JwtCreator jwtCreator) {
        this.userDetailsService = userDetailsService;
        //this.jwtHelper = jwtHelper;
        this.jwtCreator = jwtCreator;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;

        // header: Authorization  Bearer [jwt]
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token != null && jwtCreator.isTokenValid(token)) {
            Claims claims = jwtCreator.getClaims(token);
            String username = claims.getSubject();

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            log.info("UserDetails authorities: " + userDetails.getAuthorities());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()

            );


            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);

//        log.info("Inside JWT filter");
//        try {
//            final String authorizationHeader = request.getHeader("Authorization");
//
//            System.out.println("Print Auth header: " + authorizationHeader);
//            String jwt = null;
//            String useremail = null;
//            if (Objects.nonNull(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
//                jwt = authorizationHeader.substring(7);
//                System.out.println("JWT Tokwn ONLY: " + jwt);
//                useremail = jwtHelper.extractUseremail(jwt);
//            }
//
//            System.out.println("Security Context: " + SecurityContextHolder.getContext().getAuthentication());
//            if (Objects.nonNull(useremail) && SecurityContextHolder.getContext().getAuthentication() == null) {
//                System.out.println("Context username:" + useremail);
//                UserDetails userDetails = this.userDetailsService.loadUserByUsername(useremail);
//                System.out.println("Context user details: " + userDetails);
//                boolean isTokenValidated = jwtHelper.validateToken(jwt, userDetails);
//                System.out.println("Is token validated: " + isTokenValidated);
//
//                if (isTokenValidated) {
//                    System.out.println("UerDetails authorities: " + userDetails.getAuthorities());
//                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
//                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
//                }
//            } else {
//                throw new BadCredentialsException("Bearer token not set correctly");
//            }
//        } catch (ExpiredJwtException jwtException) {
//            request.setAttribute("exception", jwtException);
//        } catch (BadCredentialsException | UnsupportedJwtException | MalformedJwtException e) {
//            log.error("Filter exception: {}", e.getMessage());
//            request.setAttribute("exception", e);
//        }
//
//        filterChain.doFilter(request, response);

    }
}

