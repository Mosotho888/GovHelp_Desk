package za.gov.helpdesk.auth.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

@Component
@Getter
public class AuthMetrics {

    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter tokenRefreshed;
    private final Counter logout;
    private final Counter passwordResetRequested;
    private final Counter passwordResetConfirmed;
    private final Counter rateLimitExceeded;

    public AuthMetrics(final MeterRegistry registry) {

        this.loginSuccess =
                Counter.builder("helpdesk.auth.login.success")
                        .description("Successful login authentications")
                        .register(registry);

        this.loginFailure =
                Counter.builder("helpdesk.auth.login.failure")
                        .description("Failed login attempts (bad credentials or locked account)")
                        .register(registry);

        this.tokenRefreshed =
                Counter.builder("helpdesk.auth.token.refreshed")
                        .description("JWT access tokens successfully refreshed")
                        .register(registry);

        this.logout =
                Counter.builder("helpdesk.auth.logout")
                        .description("Explicit logout events (refresh tokens revoked)")
                        .register(registry);

        this.passwordResetRequested =
                Counter.builder("helpdesk.auth.password.reset.requested")
                        .description("Password reset OTPs requested")
                        .register(registry);

        this.passwordResetConfirmed =
                Counter.builder("helpdesk.auth.password.reset.confirmed")
                        .description("Passwords successfully changed via OTP confirmation")
                        .register(registry);

        this.rateLimitExceeded =
                Counter.builder("helpdesk.auth.rate.limit.exceeded")
                        .description("Requests throttled by the Bucket4j rate-limiting filter")
                        .register(registry);
    }

    public void incrementLoginSuccess() {

        this.loginSuccess.increment();
    }

    public void incrementLoginFailure() {

        this.loginFailure.increment();
    }

    public void incrementTokenRefreshed() {

        this.tokenRefreshed.increment();
    }

    public void incrementLogout() {

        this.logout.increment();
    }

    public void incrementPasswordResetConfirmed() {

        this.passwordResetConfirmed.increment();
    }

    public void incrementPasswordResetRequested() {

        this.passwordResetRequested.increment();
    }

    public void incrementRateLimitExceeded() {

        this.rateLimitExceeded.increment();
    }
}
