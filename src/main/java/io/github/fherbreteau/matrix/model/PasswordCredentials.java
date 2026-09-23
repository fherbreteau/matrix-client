package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Password-based credentials for the {@code m.login.password} flow. The password never appears in
 * {@link #toString()}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/password-based">Matrix
 *     specification</a>
 */
public record PasswordCredentials(String identifier, String password) implements Credentials {

  /** Creates password credentials for the given user identifier. */
  public PasswordCredentials {
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("identifier is required");
    }
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("password is required");
    }
  }

  @Override
  public JsonValue toJson() {
    return new JsonObject()
        .put("type", "m.login.password")
        .put("identifier", new JsonObject().put("type", "m.id.user").put("user", identifier))
        .put("password", password);
  }

  /** Returns a representation that never includes the password. */
  @Override
  public String toString() {
    return "PasswordCredentials[identifier=" + identifier + ", password=***]";
  }
}
