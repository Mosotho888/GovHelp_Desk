package za.gov.helpdesk.unit.services.auth;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import za.gov.helpdesk.auth.service.impl.OtpGeneratorServiceImpl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("OtpGeneratorServiceImpl unit tests")
class OtpGeneratorServiceImplTest {

    private final OtpGeneratorServiceImpl generator = new OtpGeneratorServiceImpl();

    @RepeatedTest(20)
    @DisplayName("generate() always produces a 6-digit numeric string")
    void generate_repeatedCalls_alwaysSixDigits() {
        final String otp = generator.generate();

        assertThat(otp).hasSize(6).matches("\\d{6}");
    }

    @Test
    @DisplayName("generate() produces values within the intended 6-digit range")
    void generate_manyCalls_staysWithinExpectedRange() {
        for (int i = 0; i < 500; i++) {
            final int value = Integer.parseInt(generator.generate());
            assertThat(value).isBetween(100_000, 999_999);
        }
    }

    @Test
    @DisplayName("generate() produces varied values across repeated calls (not a fixed constant)")
    void generate_manyCalls_producesVariety() {
        final Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            generated.add(generator.generate());
        }

        // With a 900,000-value range, 100 draws should almost certainly not collapse to <2 distinct
        // values; this guards against a broken generator that always returns the same OTP.
        assertThat(generated.size()).isGreaterThan(1);
    }
}
