package com.example.ses;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/** An accepted email, as captured by the simulated delivery. */
public record Email(
        String id,
        String from,
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String text,
        String html,
        Instant sentAt) {

    /** Fixed allowance per email for object and map overhead. */
    private static final long BASE_SIZE_BYTES = 512;

    public Email {
        to = List.copyOf(to);
        cc = List.copyOf(cc);
        bcc = List.copyOf(bcc);
    }

    /** Approximate heap use, counting two bytes per character as the worst case. */
    long approximateSizeBytes() {
        long chars = Stream.of(List.of(from, subject), to, cc, bcc).flatMap(List::stream).mapToLong(String::length).sum()
                + (text == null ? 0 : text.length())
                + (html == null ? 0 : html.length());
        return BASE_SIZE_BYTES + 2 * chars;
    }
}
