package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Flexible nested body for uploading E2EE keys or room-key backup data.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysupload">Matrix
 *     specification</a>
 */
public record EncryptionRequest(JsonObject payload) {

  /**
   * Wraps a JSON object without transforming any nested cryptographic values.
   *
   * @param value the raw body
   * @return a request wrapper
   * @throws IllegalArgumentException if the body is not an object
   */
  public static EncryptionRequest of(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("encryption request must be a JSON object");
    }
    return new EncryptionRequest(value.asObject());
  }

  /** Returns the body unchanged. */
  public JsonObject toJson() {
    return payload;
  }
}
