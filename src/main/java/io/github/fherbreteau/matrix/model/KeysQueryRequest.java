package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Raw nested request body for a device-key query.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysquery">Matrix
 *     specification</a>
 */
public record KeysQueryRequest(JsonObject payload) {

  /**
   * Wraps a device-key query JSON object.
   *
   * @param value the raw request body
   * @return the request wrapper
   * @throws IllegalArgumentException if value is not an object
   */
  public static KeysQueryRequest of(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("keys query request must be a JSON object");
    }
    return new KeysQueryRequest(value.asObject());
  }

  /** Returns the unmodified query body. */
  public JsonObject toJson() {
    return payload;
  }
}
