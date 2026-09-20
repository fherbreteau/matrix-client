package io.github.fherbreteau.matrix.transport;

/**
 * Thrown when the HTTP layer fails to complete a request (connection error, DNS failure, ...). This
 * is generally a retryable failure.
 */
public class UncheckedTransportException extends TransportException {

  /**
   * Creates a connection-failure exception with the given message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public UncheckedTransportException(String message, Throwable cause) {
    super(message, cause);
  }
}
