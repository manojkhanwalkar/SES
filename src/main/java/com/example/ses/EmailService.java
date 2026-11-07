package com.example.ses;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Validates emails and "delivers" them by capturing them in the store. Nothing leaves the process. */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailStore store;
    private final Clock clock = Clock.systemUTC();

    public EmailService(EmailStore store) {
        this.store = store;
    }

    public Email send(SendEmailRequest request) {
        List<String> errors = EmailValidator.validate(request);
        if (!errors.isEmpty()) {
            throw new ApiException.Invalid(errors);
        }
        Email email = new Email(UUID.randomUUID().toString(), request.from(), request.to(), request.cc(),
                request.bcc(), request.subject(), request.text(), request.html(), clock.instant());
        store.save(email);
        log.info("Captured email {} for {} recipient(s)",
                email.id(), email.to().size() + email.cc().size() + email.bcc().size());
        return email;
    }

    public Email get(String id) {
        return store.find(id).orElseThrow(ApiException.NotFound::new);
    }

    public List<EmailSummary> list(int limit) {
        return store.newest(limit).stream().map(EmailSummary::of).toList();
    }
}
