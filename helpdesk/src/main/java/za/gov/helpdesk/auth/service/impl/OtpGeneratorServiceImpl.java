package za.gov.helpdesk.auth.service.impl;

import org.springframework.stereotype.Service;
import za.gov.helpdesk.auth.service.OtpGeneratorService;

import java.security.SecureRandom;

@Service
public class OtpGeneratorServiceImpl implements OtpGeneratorService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate() {
        int otp = 100_000 + RANDOM.nextInt(900_000);
        return String.valueOf(otp);
    }
}
