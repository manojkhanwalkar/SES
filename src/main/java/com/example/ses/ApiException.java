package com.example.ses;

import java.util.List;
import org.springframework.http.HttpStatus;

/** A failure with a defined HTTP response. */
abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> details;

    private ApiException(HttpStatus status, String code, String message, List<String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    HttpStatus status() {
        return status;
    }

    ApiError toError() {
        return new ApiError(code, getMessage(), details);
    }

    static final class Invalid extends ApiException {
        Invalid(List<String> details) {
            super(HttpStatus.BAD_REQUEST, "invalid_request", "The request is invalid", details);
        }
    }

    static final class NotFound extends ApiException {
        NotFound() {
            super(HttpStatus.NOT_FOUND, "not_found", "No such email", List.of());
        }
    }

    static final class TooLarge extends ApiException {
        TooLarge(int maxBytes) {
            super(HttpStatus.CONTENT_TOO_LARGE, "too_large", "Request body exceeds " + maxBytes + " bytes", List.of());
        }
    }
}
