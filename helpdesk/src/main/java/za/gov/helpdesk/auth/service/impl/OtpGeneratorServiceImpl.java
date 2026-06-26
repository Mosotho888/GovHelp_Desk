package za.gov.helpdesk.auth.service.impl;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import za.gov.helpdesk.auth.service.OtpGeneratorService;

@Service
public class OtpGeneratorServiceImpl implements OtpGeneratorService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SIX_DIGIT_CODE_MIN = 100_000;
    private static final int SIX_DIGIT_CODE_MAX = 900_000;

    @Override
    public String generate() {
        final int otp = SIX_DIGIT_CODE_MIN + RANDOM.nextInt(SIX_DIGIT_CODE_MAX);
        return String.valueOf(otp);
    }
}
