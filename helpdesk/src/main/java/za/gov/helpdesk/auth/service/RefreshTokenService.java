package za.gov.helpdesk.auth.service;

import za.gov.helpdesk.auth.model.RefreshToken;
import za.gov.helpdesk.users.model.User;

public interface RefreshTokenService {

    void store(String rawToken, User user);

    RefreshToken validate(String rawToken);

    void revokeAll(User user);

    void purgeExpiredTokens();
}
