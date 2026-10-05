package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Raw nested request body for claiming one-time or fallback keys.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysclaim">Matrix
 *     specification</a>
 */
public record KeysClaimRequest(JsonObject payload) {

  /**
   * Wraps a key-claim JSON object.
   *
   * @param value the raw request body
   * @return the request wrapper
   * @throws IllegalArgumentException if value is not an object
   */
  public static KeysClaimRequest of(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("keys claim request must be a JSON object");
    }
    return new KeysClaimRequest(value.asObject());
  }

  /** Returns the unmodified claim body. */
  public JsonObject toJson() {
    return payload;
  }
}
