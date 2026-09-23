package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Login credentials. Sealed so new authentication flows (such as OAuth 2.0) can be added later
 * without breaking callers. Implementations never expose secrets in {@code toString()}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#login">Matrix specification</a>
 */
public sealed interface Credentials permits PasswordCredentials {

  /**
   * Serializes the credentials as a login request body.
   *
   * @return the login request body for these credentials
   */
  JsonValue toJson();

  /** Returns a representation that never includes secrets. */
  @Override
  String toString();
}
