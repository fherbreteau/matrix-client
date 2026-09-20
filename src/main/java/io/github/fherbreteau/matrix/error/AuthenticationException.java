package io.github.fherbreteau.matrix.error;

/**
 * Thrown when authentication fails: invalid credentials, deactivated accounts, unknown or expired
 * tokens, or missing session. The message never contains credentials or access tokens.
 */
public final class AuthenticationException extends MatrixException {

  /** Creates an authentication exception with the given error code and message. */
  public AuthenticationException(String errcode, String message) {
    super(errcode, message);
  }
}
