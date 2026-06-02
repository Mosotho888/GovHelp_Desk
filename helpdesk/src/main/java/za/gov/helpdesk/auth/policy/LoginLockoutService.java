package za.gov.helpdesk.auth.policy;

public interface LoginLockoutService {

    void recordFailedAttempt(String email);
}
