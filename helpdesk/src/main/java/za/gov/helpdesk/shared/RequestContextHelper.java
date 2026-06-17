package za.gov.helpdesk.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestContextHelper {

    private static final int INDEX = 0;

    public String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");

            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[INDEX].trim();
            }

            return request.getRemoteAddr();

        } catch (IllegalStateException e) {
            // No request context - called from a scheduled task or async thread
            return "system";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
