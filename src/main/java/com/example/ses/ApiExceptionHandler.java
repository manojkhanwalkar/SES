package com.example.ses;

import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

/** Gives every failure, including those raised by Spring MVC itself, the {@link ApiError} shape. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException e) {
        return ResponseEntity.status(e.status()).body(e.toError());
    }

    @ExceptionHandler(JacksonException.class)
    ResponseEntity<ApiError> handleMalformedJson(JacksonException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("invalid_request", "The request body is not valid JSON for this API", List.of()));
    }

    /** Spring MVC failures such as 405 and 415. Uses the status text so no request input is echoed back. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus resolved = HttpStatus.valueOf(status.value());
        String code = status.value() == 400
                ? "invalid_request"
                : resolved.name().toLowerCase(Locale.ROOT);
        ApiError error = new ApiError(code, resolved.getReasonPhrase(), List.of());
        return super.handleExceptionInternal(e, error, headers, status, request);
    }
}
