package za.gov.helpdesk.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPolicyProvider {

    @Value("${rate-limit.capacity.unauthenticated}")
    private long unauthenticatedCapacity;

    @Value("${rate-limit.capacity.user}")
    private long userCapacity;

    @Value("${rate-limit.capacity.agent}")
    private long agentCapacity;

    @Value("${rate-limit.capacity.admin}")
    private long adminCapacity;

    public long capacityFor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return unauthenticatedCapacity;
        }

        if (hasRole(auth, "ROLE_ADMIN")) {
            return adminCapacity;
        }
        if (hasRole(auth, "ROLE_AGENT")) {
            return agentCapacity;
        }

        return userCapacity;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
