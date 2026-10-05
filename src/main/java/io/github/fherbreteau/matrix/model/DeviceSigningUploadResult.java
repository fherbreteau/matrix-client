package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Raw cross-signing and device-signing upload acknowledgement.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysdevice_signingupload">Matrix
 *     specification</a>
 */
public record DeviceSigningUploadResult(JsonObject payload) {

  /**
   * Parses a successful upload response.
   *
   * @param value response JSON
   * @return validated response
   * @throws IllegalArgumentException if the response is not an object
   */
  public static DeviceSigningUploadResult from(JsonValue value) {
    return new DeviceSigningUploadResult(ModelJson.object(value, "device-signing upload response"));
  }

  /** Returns the unmodified success response. */
  public JsonObject toJson() {
    return payload;
  }
}
