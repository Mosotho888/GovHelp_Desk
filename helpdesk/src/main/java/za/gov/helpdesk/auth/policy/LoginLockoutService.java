package za.gov.helpdesk.auth.policy;

import za.gov.helpdesk.users.model.User;

public interface LoginLockoutService {

    void recordFailedAttempt(String email);

    void resetFailedAttempts(User user);
}
