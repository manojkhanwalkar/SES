package com.example.ses;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Checks a send request and returns every problem found, so clients can fix them all at once. */
final class EmailValidator {

    static final int MAX_RECIPIENTS = 50;
    private static final int MAX_ADDRESS_LENGTH = 254;
    private static final int MAX_SUBJECT_LENGTH = 998;

    private static final Pattern ADDRESS = Pattern.compile(
            "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
                    + "@[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)+");
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");

    private EmailValidator() {
    }

    static List<String> validate(SendEmailRequest request) {
        List<String> errors = new ArrayList<>();

        if (!isValidAddress(request.from())) {
            errors.add("from: must be a valid email address");
        }
        checkRecipients("to", request.to(), errors);
        checkRecipients("cc", request.cc(), errors);
        checkRecipients("bcc", request.bcc(), errors);

        int recipients = request.to().size() + request.cc().size() + request.bcc().size();
        if (recipients == 0) {
            errors.add("to: at least one recipient is required across to, cc and bcc");
        } else if (recipients > MAX_RECIPIENTS) {
            errors.add("to: at most " + MAX_RECIPIENTS + " recipients are allowed across to, cc and bcc");
        }

        String subject = request.subject();
        if (subject == null || subject.isBlank()) {
            errors.add("subject: is required");
        } else if (subject.length() > MAX_SUBJECT_LENGTH) {
            errors.add("subject: must be at most " + MAX_SUBJECT_LENGTH + " characters");
        } else if (CONTROL_CHARS.matcher(subject).find()) {
            errors.add("subject: must not contain control characters");
        }

        if (isBlank(request.text()) && isBlank(request.html())) {
            errors.add("text: at least one of text and html is required");
        }
        return errors;
    }

    private static void checkRecipients(String field, List<String> addresses, List<String> errors) {
        for (int i = 0; i < addresses.size(); i++) {
            if (!isValidAddress(addresses.get(i))) {
                errors.add(field + "[" + i + "]: must be a valid email address");
            }
        }
    }

    private static boolean isValidAddress(String address) {
        return address != null && address.length() <= MAX_ADDRESS_LENGTH && ADDRESS.matcher(address).matches();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
