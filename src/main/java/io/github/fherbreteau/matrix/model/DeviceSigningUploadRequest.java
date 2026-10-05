package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Uploads master, self-signing, and user-signing keys, with optional UI-auth response data.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysdevice_signingupload">Matrix
 *     specification</a>
 */
public record DeviceSigningUploadRequest(JsonObject payload) {

  /**
   * Wraps a cross-signing request body.
   *
   * @param value raw request JSON
   * @return cross-signing upload wrapper
   * @throws IllegalArgumentException if value is not an object
   */
  public static DeviceSigningUploadRequest of(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("device-signing request must be a JSON object");
    }
    return new DeviceSigningUploadRequest(value.asObject());
  }

  /** Returns raw key and UI-auth data. */
  public JsonObject toJson() {
    return payload;
  }
}
