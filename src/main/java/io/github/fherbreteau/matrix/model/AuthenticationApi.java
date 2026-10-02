package io.github.fherbreteau.matrix.model;

/**
 * Authentication API used to obtain a Matrix access token.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#authentication-api-discovery">Matrix
 *     specification</a>
 */
public enum AuthenticationApi {
  LEGACY,
  OAUTH
}
