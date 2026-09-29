package io.github.fherbreteau.matrix.model;

import java.time.Duration;
import java.util.UUID;

/**
 * Redacted metadata describing a single HTTP request attempt.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#rate-limiting">Matrix
 *     specification</a>
 */
public record RequestAttempt(
    UUID correlationId,
    String method,
    String endpoint,
    int number,
    Integer statusCode,
    Duration duration,
    Duration retryDelay,
    Outcome outcome) {

  /** Result of the HTTP attempt. */
  public enum Outcome {
    SUCCEEDED,
    RETRYING,
    FAILED,
    INTERRUPTED
  }
}
