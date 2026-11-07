package com.example.ses;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailValidatorTest {

    private static SendEmailRequest withFrom(String from) {
        return new SendEmailRequest(from, List.of("to@example.com"), null, null, "Subject", "Body", null);
    }

    private static SendEmailRequest withSubject(String subject) {
        return new SendEmailRequest("a@example.com", List.of("to@example.com"), null, null, subject, "Body", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@example.com", "first.last+tag@sub.example.co.uk", "o'neil@example.com"})
    void acceptsValidAddresses(String address) {
        assertThat(EmailValidator.validate(withFrom(address))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plain", "a@localhost", "a@@example.com", ".a@example.com", "a..b@example.com",
            "a@-example.com", "a b@example.com", "a@example.com\r\nBcc: x@example.com"})
    void rejectsInvalidAddresses(String address) {
        assertThat(EmailValidator.validate(withFrom(address))).containsExactly("from: must be a valid email address");
    }

    @Test
    void rejectsHeaderInjectionInSubject() {
        assertThat(EmailValidator.validate(withSubject("Hi\r\nBcc: x@example.com")))
                .containsExactly("subject: must not contain control characters");
    }

    @Test
    void ccAloneIsEnoughRecipients() {
        SendEmailRequest request =
                new SendEmailRequest("a@example.com", null, List.of("c@example.com"), null, "Subject", null, "<p>x</p>");
        assertThat(EmailValidator.validate(request)).isEmpty();
    }

    @Test
    void enforcesRecipientLimitAcrossAllFields() {
        List<String> addresses = Collections.nCopies(EmailValidator.MAX_RECIPIENTS, "to@example.com");
        SendEmailRequest request =
                new SendEmailRequest("a@example.com", addresses, List.of("c@example.com"), null, "Subject", "Body", null);
        assertThat(EmailValidator.validate(request)).hasSize(1).first().asString().contains("at most 50");
    }

    @Test
    void nullRecipientIsReportedNotThrown() {
        SendEmailRequest request = new SendEmailRequest("a@example.com", Arrays.asList("to@example.com", null),
                null, null, "Subject", "Body", null);
        assertThat(EmailValidator.validate(request)).containsExactly("to[1]: must be a valid email address");
    }

    @Test
    void requiresABody() {
        SendEmailRequest request =
                new SendEmailRequest("a@example.com", List.of("to@example.com"), null, null, "Subject", " ", null);
        assertThat(EmailValidator.validate(request)).containsExactly("text: at least one of text and html is required");
    }
}
