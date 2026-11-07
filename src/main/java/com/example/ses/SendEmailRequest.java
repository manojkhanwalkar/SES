package com.example.ses;

import java.util.List;

/** Body of POST /v1/emails. At least one of {@code text} and {@code html} is required. */
public record SendEmailRequest(
        String from,
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String text,
        String html) {

    public SendEmailRequest {
        to = to == null ? List.of() : to;
        cc = cc == null ? List.of() : cc;
        bcc = bcc == null ? List.of() : bcc;
    }
}
