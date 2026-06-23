package za.gov.helpdesk.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Policy provider component responsible for determining API rate-limiting token bucket capacities.
 * Dynamically resolves permissible throughput volume thresholds based on the authenticated client
 * security principal context and assigned authority roles.
 */
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

    /**
     * Resolves the maximum token bucket request capacity assigned to a given security
     * authentication context. Maps unauthenticated or anonymous requests to restrictive baseline
     * tiers, while granting progressively higher throughput ceilings to authenticated Users,
     * Agents, and Administrators.
     *
     * @param auth the active {@link Authentication} security principal context to inspect
     * @return the long primitive value representing total allowable requests inside the evaluation
     *     window
     */
    public long capacityFor(final Authentication auth) {
        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
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

    /**
     * Helper method to inspect the authentication authority stream for a specific targeted role
     * mapping.
     *
     * @param auth the active {@link Authentication} token wrapper context
     * @param role the explicit target authority string matching pattern (e.g., "ROLE_ADMIN")
     * @return true if the authority stream contains an exact match, false otherwise
     */
    private boolean hasRole(final Authentication auth, final String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
