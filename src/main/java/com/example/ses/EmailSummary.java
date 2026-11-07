package com.example.ses;

import java.time.Instant;
import java.util.List;

/** List view of an email, without bodies or the cc and bcc recipients. */
public record EmailSummary(String id, String from, List<String> to, String subject, Instant sentAt) {

    static EmailSummary of(Email email) {
        return new EmailSummary(email.id(), email.from(), email.to(), email.subject(), email.sentAt());
    }
}
