package com.example.ses;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmailStoreTest {

    private static Email email(String id, int textLength) {
        return new Email(id, "a@example.com", List.of("b@example.com"), List.of(), List.of(), "s",
                "x".repeat(textLength), null, Instant.EPOCH);
    }

    private static EmailStore storeWithBudget(long maxBytes) {
        return new EmailStore(new SesProperties("0123456789abcdef", 1024, maxBytes));
    }

    @Test
    void evictsOldestWhenOverBudget() {
        long oneEmail = email("x", 1000).approximateSizeBytes();
        EmailStore store = storeWithBudget(2 * oneEmail);

        store.save(email("1", 1000));
        store.save(email("2", 1000));
        store.save(email("3", 1000));

        assertThat(store.find("1")).isEmpty();
        assertThat(store.newest(10)).extracting(Email::id).containsExactly("3", "2");
    }

    @Test
    void keepsNewestEmailEvenIfItExceedsBudget() {
        EmailStore store = storeWithBudget(1);
        store.save(email("1", 1000));
        store.save(email("2", 1000));

        assertThat(store.newest(10)).extracting(Email::id).containsExactly("2");
    }
}
