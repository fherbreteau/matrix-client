package io.github.fherbreteau.matrix.error;

/**
 * Thrown when a homeserver discovery or capability response is invalid or unsupported. Unlike
 * transport failures, this indicates a homeserver that answers but with a response that does not
 * conform to the Matrix spec.
 */
public final class DiscoveryException extends MatrixException {

  /**
   * Creates a discovery exception with the given message.
   *
   * @param message the error message
   */
  public DiscoveryException(String message) {
    super("M_UNRECOGNIZED", message);
  }
}
