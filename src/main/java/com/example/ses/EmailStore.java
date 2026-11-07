package com.example.ses;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Thread-safe in-memory store. Evicts the oldest emails once the memory budget is exceeded. */
@Component
public class EmailStore {

    private final LinkedHashMap<String, Email> emails = new LinkedHashMap<>();
    private final long maxBytes;
    private long usedBytes;

    public EmailStore(SesProperties properties) {
        this.maxBytes = properties.maxStoreBytes();
    }

    public synchronized void save(Email email) {
        emails.put(email.id(), email);
        usedBytes += email.approximateSizeBytes();
        // Always keep the newest email, so the id just returned to the client can be read back.
        while (usedBytes > maxBytes && emails.size() > 1) {
            usedBytes -= emails.pollFirstEntry().getValue().approximateSizeBytes();
        }
    }

    public synchronized Optional<Email> find(String id) {
        return Optional.ofNullable(emails.get(id));
    }

    /** Returns up to {@code limit} emails, newest first. */
    public synchronized List<Email> newest(int limit) {
        return emails.sequencedValues().reversed().stream().limit(limit).toList();
    }
}
