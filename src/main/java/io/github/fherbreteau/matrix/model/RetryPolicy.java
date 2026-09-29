package io.github.fherbreteau.matrix.model;

import java.util.Locale;

/**
 * Bounded automatic retry policy for idempotent HTTP requests.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#rate-limiting">Matrix
 *     specification</a>
 */
public record RetryPolicy(
    int maxRetries, long initialDelayMs, long maxDelayMs, long maxRetryAfterMs) {

  private static final RetryPolicy DEFAULT = new RetryPolicy(2, 250, 2_000, 30_000);
  private static final RetryPolicy DISABLED = new RetryPolicy(0, 0, 0, 0);

  /** Creates a bounded policy with validated non-negative limits. */
  public RetryPolicy {
    if (maxRetries < 0
        || initialDelayMs < 0
        || maxDelayMs < initialDelayMs
        || maxRetryAfterMs < 0) {
      throw new IllegalArgumentException(
          "retry counts and delays must be non-negative and ordered");
    }
  }

  /** Returns the default policy: two retries, starting at 250 ms, capped at 2 s. */
  public static RetryPolicy defaults() {
    return DEFAULT;
  }

  /** Returns a policy which disables automatic retries. */
  public static RetryPolicy disabled() {
    return DISABLED;
  }

  /** Returns whether a request method is idempotent and may be retried by default. */
  public boolean retries(String method) {
    return switch (method.toUpperCase(Locale.ROOT)) {
      case "GET", "HEAD", "OPTIONS", "PUT", "DELETE" -> true;
      default -> false;
    };
  }

  /** Returns capped exponential delay, using the server hint when it is non-negative. */
  public long delayMs(int retryNumber, Long retryAfterMs) {
    if (retryAfterMs != null && retryAfterMs >= 0) {
      return Math.min(retryAfterMs, maxRetryAfterMs);
    }
    long delay = initialDelayMs;
    for (int attempt = 1; attempt < retryNumber && delay < maxDelayMs; attempt++) {
      delay = delay > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : Math.min(delay * 2, maxDelayMs);
    }
    return Math.min(delay, maxDelayMs);
  }
}
