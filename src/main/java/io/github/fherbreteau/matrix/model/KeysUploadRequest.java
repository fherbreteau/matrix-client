package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Request for uploading device identity, one-time, and fallback keys.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysupload">Matrix
 *     specification</a>
 */
public record KeysUploadRequest(JsonObject payload) {

  /**
   * Wraps a JSON body for key upload.
   *
   * @param value raw key upload JSON
   * @return the typed wrapper
   * @throws IllegalArgumentException if the value is not an object
   */
  public static KeysUploadRequest of(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("keys upload request must be a JSON object");
    }
    return new KeysUploadRequest(value.asObject());
  }

  /** Returns the original request body. */
  public JsonObject toJson() {
    return payload;
  }
}
