package io.github.fherbreteau.matrix.model;

/**
 * Receives per-attempt request metadata for metrics or diagnostics without a metrics dependency.
 * Implementations should return quickly.
 */
@FunctionalInterface
public interface RequestObserver {

  /**
   * Called after each request attempt; the event excludes headers, body and query values.
   *
   * @param attempt redacted attempt metadata
   */
  void onAttempt(RequestAttempt attempt);

  /**
   * Returns a no-op observer.
   *
   * @return observer that discards attempt metadata
   */
  static RequestObserver noop() {
    return _ -> {};
  }
}
