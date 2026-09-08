package za.gov.helpdesk.auth.service;

@FunctionalInterface
public interface OtpGeneratorService {

    String generate();
}
