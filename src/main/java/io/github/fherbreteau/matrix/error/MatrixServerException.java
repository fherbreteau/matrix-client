package io.github.fherbreteau.matrix.error;

import java.util.Map;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Thrown when the homeserver returns a non-2xx HTTP response. Carries the
 * HTTP status, the Matrix {@code errcode}/{@code error} fields, any
 * additional (unknown) fields of the error body, and whether the failure is
 * retryable.
 */
public class MatrixServerException extends MatrixException {

    private final int statusCode;
    private final transient Map<String, JsonValue> fields;
    private final boolean retryable;

    public MatrixServerException(int statusCode, String errcode, String message) {
        this(statusCode, errcode, message, Map.of(), isRetryableStatus(statusCode));
    }

    public MatrixServerException(int statusCode,
        String errcode,
        String message,
        Map<String, JsonValue> fields,
        boolean retryable) {
        super(errcode, message);
        this.statusCode = statusCode;
        this.fields = Map.copyOf(fields);
        this.retryable = retryable;
    }

    /**
     * Builds the exception from an HTTP status and the parsed JSON error
     * body. Unknown fields of the body are preserved.
     */
    public static MatrixServerException fromResponse(int statusCode, JsonValue body) {
        return fromResponse(statusCode, body, null);
    }

    /**
     * Builds the exception from an HTTP status, the parsed JSON error body
     * and the response headers (used for {@code Retry-After} information).
     */
    public static MatrixServerException fromResponse(int statusCode,
        JsonValue body,
        Map<String, String> headers) {
        String errcode = null;
        String message = null;
        Map<String, JsonValue> additional = Map.of();
        if (body != null && body.isObject()) {
            JsonObject obj = body.asObject();
            var err = obj.get("errcode");
            var msg = obj.get("error");
            if (err != null && err.isString()) {
                errcode = err.asString();
            }
            if (msg != null && msg.isString()) {
                message = msg.asString();
            }
            additional = unknownFields(obj);
        }
        if (errcode == null) {
            errcode = "M_UNRECOGNIZED";
        }
        if (message == null) {
            message = "HTTP " + statusCode;
        }
        if (statusCode == 429) {
            Long retryAfterMs = extractRetryAfterMs(headers);
            return new RateLimitedException(errcode, message, additional, retryAfterMs);
        }
        return new MatrixServerException(statusCode, errcode, message, additional, isRetryableStatus(statusCode));
    }

    private static Map<String, JsonValue> unknownFields(JsonObject obj) {
        var additional = new java.util.LinkedHashMap<String, JsonValue>();
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            if (!"errcode".equals(entry.getKey()) && !"error".equals(entry.getKey())) {
                additional.put(entry.getKey(), entry.getValue());
            }
        }
        return additional;
    }

    private static Long extractRetryAfterMs(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }
        String value = headers.get("retry-after");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.time.Duration.ofSeconds(Long.parseLong(value.strip())).toMillis();
        } catch (NumberFormatException _) {
            return null;
        }
    }

    static boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429
                || statusCode == 500 || statusCode == 502
                || statusCode == 503 || statusCode == 504;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the additional (unknown) fields of the Matrix error body,
     * beyond {@code errcode} and {@code error}.
     */
    public Map<String, JsonValue> getFields() {
        return fields == null ? Map.of() : fields;
    }

    /**
     * Returns whether the same request can be retried with a chance of
     * success (timeouts, rate limits, transient server errors).
     */
    public boolean isRetryable() {
        return retryable;
    }
}
