package za.gov.helpdesk.config.security;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import za.gov.helpdesk.auth.metrics.AuthMetrics;
import za.gov.helpdesk.exception.RateLimitExceededException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int EXPIRE_DURATION = 2;
    private static final int MAX_SIZE = 50_000;
    private static final int NUM_TOKENS = 1;
    private static final int INDEX = 0;
    private static final int RESET_DURATION = 1;

    private final RateLimitPolicyProvider policyProvider;
    private final AuthMetrics authMetrics;
    private final Environment environment;
    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver resolver;

    private final Cache<String, Bucket> buckets =
            Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofHours(EXPIRE_DURATION))
                    .maximumSize(MAX_SIZE)
                    .build();

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain)
            throws ServletException, IOException {

        if (environment.acceptsProfiles(Profiles.of("test"))) {
            filterChain.doFilter(request, response);
            return;
        }

        final String key = resolveKey(request);
        final Bucket bucket = buckets.get(key, k -> createBucket(request));

        if (bucket.tryConsume(NUM_TOKENS)) {

            final long remaining = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
            filterChain.doFilter(request, response);
        } else {
            authMetrics.incrementRateLimitExceeded();

            resolver.resolveException(
                    request,
                    response,
                    null,
                    new RateLimitExceededException(
                            "Too many requests. Please slow down and retry after one hour"));
        }
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();

        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/prometheus")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    private String resolveKey(final HttpServletRequest request) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }

        final String forwarded = request.getHeader("X-Forwarded-For");
        return "ip:"
                + (forwarded != null
                        ? forwarded.split(",")[INDEX].trim()
                        : request.getRemoteAddr());
    }

    private Bucket createBucket(final HttpServletRequest request) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final long capacity = policyProvider.capacityFor(auth);

        return Bucket.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(capacity)
                                .refillGreedy(capacity, Duration.ofHours(RESET_DURATION))
                                .build())
                .build();
    }
}
