package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Raw nested response containing claimed one-time or fallback keys.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysclaim">Matrix
 *     specification</a>
 */
public record KeysClaimResponse(JsonObject payload, JsonValue raw) {

  /**
   * Parses a key-claim response and validates its required key map.
   *
   * @param value response JSON
   * @return validated raw key response
   * @throws IllegalArgumentException if one_time_keys is missing or malformed
   */
  public static KeysClaimResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "keys claim response");
    JsonValue oneTimeKeys = object.get("one_time_keys");
    if (oneTimeKeys == null || !oneTimeKeys.isObject()) {
      throw new IllegalArgumentException("keys claim response must contain one_time_keys object");
    }
    JsonValue failures = object.get("failures");
    if (failures != null && !failures.isObject()) {
      throw new IllegalArgumentException("keys claim failures must be an object");
    }
    return new KeysClaimResponse(object, value);
  }

  /** Returns claimed keys without transforming nested cryptographic values. */
  public JsonObject oneTimeKeys() {
    return payload.get("one_time_keys").asObject();
  }

  /** Returns optional remote homeserver failures. */
  public JsonObject failures() {
    JsonValue value = payload.get("failures");
    return value == null ? null : value.asObject();
  }
}
