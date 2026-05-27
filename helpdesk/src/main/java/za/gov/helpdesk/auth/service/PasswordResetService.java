package za.gov.helpdesk.auth.service;

import za.gov.helpdesk.auth.dto.request.PasswordResetConfirmRequest;
import za.gov.helpdesk.auth.dto.request.PasswordResetRequest;

public interface PasswordResetService {

    void requestReset(PasswordResetRequest request);

    void confirmReset(PasswordResetConfirmRequest request);

    void purgeExpiredTokens();
}
