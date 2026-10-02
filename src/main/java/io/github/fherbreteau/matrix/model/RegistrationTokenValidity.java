package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Validity of a Matrix registration token, retaining unknown response fields.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1registermloginregistration_tokenvalidity">Matrix
 *     specification</a>
 */
public record RegistrationTokenValidity(boolean valid, JsonValue raw) {

  /** Parses a registration token validity response. */
  public static RegistrationTokenValidity from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("Registration token validity must be a JSON object");
    }
    JsonValue valid = value.asObject().get("valid");
    if (valid == null || !valid.isBoolean()) {
      throw new IllegalArgumentException("Registration token validity must contain valid");
    }
    return new RegistrationTokenValidity(valid.asBoolean(), value);
  }
}
