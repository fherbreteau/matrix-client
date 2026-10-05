package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Unclaimed one-time key counts returned after key upload.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysupload">Matrix
 *     specification</a>
 */
public record KeysUploadResponse(JsonObject oneTimeKeyCounts, JsonObject raw) {

  /**
   * Parses the upload response.
   *
   * @param value the response JSON
   * @return key counts and the complete response
   * @throws IllegalArgumentException if one_time_key_counts is absent or malformed
   */
  public static KeysUploadResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "keys upload response");
    JsonValue counts = object.get("one_time_key_counts");
    if (counts == null || !counts.isObject()) {
      throw new IllegalArgumentException(
          "keys upload response must contain one_time_key_counts object");
    }
    return new KeysUploadResponse(counts.asObject(), object);
  }
}
