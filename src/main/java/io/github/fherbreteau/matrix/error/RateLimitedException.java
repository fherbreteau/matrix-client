package io.github.fherbreteau.matrix.error;

import java.util.Map;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Thrown when the homeserver rate-limits the request (HTTP 429,
 * {@code M_LIMIT_EXCEEDED}). Carries the parsed {@code Retry-After} delay
 * in milliseconds when the server provides one, making the failure retryable.
 */
@SuppressWarnings("java:S110")
public final class RateLimitedException extends MatrixServerException {

    private final Long retryAfterMs;

    RateLimitedException(String errcode,
                         String message,
                         Map<String, JsonValue> fields,
                         Long retryAfterMs) {
        super(429, errcode, message, fields, true);
        this.retryAfterMs = retryAfterMs;
    }

    /**
     * Returns the parsed {@code Retry-After} delay in milliseconds, or
     * {@code null} when the server did not provide one.
     */
    public Long getRetryAfterMs() {
        return retryAfterMs;
    }
}
