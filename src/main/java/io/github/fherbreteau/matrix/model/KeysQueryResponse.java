package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Flexible JSON response for a cryptographic keys query.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysquery">Matrix
 *     specification</a>
 */
public record KeysQueryResponse(JsonObject payload) {

  /**
   * Parses a query response and validates the required device key map.
   *
   * @param value response JSON
   * @return raw response with cryptographic keys preserved
   * @throws IllegalArgumentException if device_keys is missing or malformed
   */
  public static KeysQueryResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "keys query response");
    JsonValue deviceKeys = object.get("device_keys");
    if (deviceKeys == null || !deviceKeys.isObject()) {
      throw new IllegalArgumentException("keys query response must contain device_keys object");
    }
    for (String field :
        new String[] {"failures", "master_keys", "self_signing_keys", "user_signing_keys"}) {
      JsonValue fieldValue = object.get(field);
      if (fieldValue != null && !fieldValue.isObject()) {
        throw new IllegalArgumentException("keys query " + field + " must be an object");
      }
    }
    return new KeysQueryResponse(object);
  }

  /** Returns the device map. */
  public JsonObject deviceKeys() {
    return payload.get("device_keys").asObject();
  }

  /** Returns optional remote homeserver failures. */
  public JsonObject failures() {
    return optionalObject("failures");
  }

  /** Returns optional master cross-signing keys. */
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

  /** Returns complete raw response metadata. */
  public JsonObject toJson() {
    return payload;
  }

  private JsonObject optionalObject(String field) {
    JsonValue value = payload.get(field);
    return value == null ? null : value.asObject();
  }
}
