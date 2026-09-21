package za.gov.helpdesk.auth.service;

import za.gov.helpdesk.users.model.User;

public interface AuthAuditService {

    void loggedInSuccessful(User user);

    void loggedOut(User actor);

    void passwordReset(User user);

    void accountLocked(User user, int attempts);
}
