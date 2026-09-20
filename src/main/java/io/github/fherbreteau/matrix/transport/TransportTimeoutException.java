package io.github.fherbreteau.matrix.transport;

/**
 * Thrown when a request exceeds the configured request timeout. This is a retryable failure: the
 * request may not have reached the server, or the server may not have answered in time.
 */
public final class TransportTimeoutException extends TransportException {

  /** Creates a timeout exception with the given message and cause. */
  public TransportTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
