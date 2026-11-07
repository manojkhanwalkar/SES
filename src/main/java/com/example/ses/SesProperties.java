package com.example.ses;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Service settings. Each can be set through the environment, for example SES_API_KEY.
 *
 * @param apiKey        bearer token clients must present; there is no default, so startup fails without one
 * @param maxBodyBytes  largest accepted POST /v1/emails request body
 * @param maxStoreBytes approximate memory budget for stored emails; the oldest are evicted beyond it
 */
@ConfigurationProperties("ses")
public record SesProperties(
        String apiKey,
        @DefaultValue("1048576") int maxBodyBytes,
        @DefaultValue("67108864") long maxStoreBytes) {

    private static final int MIN_API_KEY_LENGTH = 16;

    public SesProperties {
        if (apiKey == null || apiKey.length() < MIN_API_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "SES_API_KEY must be set to at least " + MIN_API_KEY_LENGTH + " characters");
        }
    }
}
