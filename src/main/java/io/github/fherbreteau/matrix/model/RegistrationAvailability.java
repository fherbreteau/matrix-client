package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Whether a username is available for registration, retaining unknown response fields.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3registeravailable">Matrix
 *     specification</a>
 */
public record RegistrationAvailability(boolean available, JsonValue raw) {

  /** Parses a username availability response. */
  public static RegistrationAvailability from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("Registration availability must be a JSON object");
    }
    JsonValue available = value.asObject().get("available");
    if (available == null || !available.isBoolean()) {
      throw new IllegalArgumentException("Registration availability must contain available");
    }
    return new RegistrationAvailability(available.asBoolean(), value);
  }
}
