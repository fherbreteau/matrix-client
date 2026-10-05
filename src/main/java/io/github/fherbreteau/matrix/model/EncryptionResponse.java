package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Response wrapper for device query, key upload, key claim, and signature upload operations.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#end-to-end-encryption">Matrix
 *     specification</a>
 */
public record EncryptionResponse(JsonObject payload) {

  /**
   * Parses an E2EE response while retaining every JSON field.
   *
   * @param value response JSON
   * @return raw response wrapper
   * @throws IllegalArgumentException if the response is not an object
   */
  public static EncryptionResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("E2EE response must be a JSON object");
    }
    return new EncryptionResponse(value.asObject());
  }

  /** Returns all response fields and nested cryptographic data unchanged. */
  public JsonObject toJson() {
    return payload;
  }

  /** Returns the required device key map. */
  public JsonObject deviceKeys() {
    return requiredObject("device_keys");
  }

  /** Returns the optional failures map. */
  public JsonObject failures() {
    return optionalObject("failures");
  }

  /** Returns optional master signing keys. */
  public JsonObject masterKeys() {
    return optionalObject("master_keys");
  }

  /** Returns optional self-signing keys. */
  public JsonObject selfSigningKeys() {
    return optionalObject("self_signing_keys");
  }

  /** Returns optional user-signing keys. */
  public JsonObject userSigningKeys() {
    return optionalObject("user_signing_keys");
  }

  /** Returns optional uploaded one-time key counts. */
  public JsonObject oneTimeKeyCounts() {
    return optionalObject("one_time_key_counts");
  }

  /** Returns the required claimed one-time key map. */
  public JsonObject oneTimeKeys() {
    return requiredObject("one_time_keys");
  }

  /** Returns users whose device keys changed, when present. */
  public JsonValue changed() {
    return payload.get("changed");
  }

  /** Returns users who left, when present. */
  public JsonValue left() {
    return payload.get("left");
  }

  private JsonObject requiredObject(String field) {
    JsonValue value = payload.get(field);
    if (value == null || !value.isObject()) {
      throw new IllegalStateException("E2EE response does not contain " + field + " object");
    }
    return value.asObject();
  }

  private JsonObject optionalObject(String field) {
    JsonValue value = payload.get(field);
    if (value == null) {
      return null;
    }
    if (!value.isObject()) {
      throw new IllegalStateException("E2EE response field " + field + " must be an object");
    }
    return value.asObject();
  }
}
