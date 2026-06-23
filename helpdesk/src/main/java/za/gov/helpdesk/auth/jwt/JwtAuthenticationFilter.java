package za.gov.helpdesk.auth.jwt;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String TOKEN_REFRESH = "refresh";
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NotNull final HttpServletRequest request,
            @NotNull final HttpServletResponse response,
            @NotNull final FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTHORIZATION);

        // Skip if no Bearer token present
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractEmail(token);

            // Only process if not already authenticated
            if (userEmail != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                final UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Reject refresh tokens on protected endpoints
                if (TOKEN_REFRESH.equals(jwtService.extractTokenType(token))) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtService.isTokenValid(token, userDetails)) {
                    final UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (final Exception e) {
            // Invalid token - clear context and let Spring Security return 401
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
