package za.gov.helpdesk.shared;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RequestContextHelper {

    private static final int INDEX = 0;

    public String getClientIp() {
        try {
            final ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

            final HttpServletRequest request = attrs.getRequest();
            final String forwarded = request.getHeader("X-Forwarded-For");

            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[INDEX].trim();
            }

            return request.getRemoteAddr();

        } catch (final IllegalStateException e) {
            // No request context - called from a scheduled task or async thread
            log.debug("No request context available, returning 'system' as client IP", e);
            return "system";
        } catch (final RuntimeException e) {
            log.warn("Unexpected error extracting client IP, returning 'unknown'", e);
            return "unknown";
        }
    }
}
