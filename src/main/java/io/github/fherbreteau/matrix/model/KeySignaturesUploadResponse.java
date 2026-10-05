package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Per-key signature upload failures, if any.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keyssignaturesupload">Matrix
 *     specification</a>
 */
public record KeySignaturesUploadResponse(JsonObject failures, JsonValue raw) {

  /**
   * Parses signature upload results.
   *
   * @param value response JSON
   * @return raw per-key failures and full response
   * @throws IllegalArgumentException if the response is malformed
   */
  public static KeySignaturesUploadResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "key signatures response");
    JsonValue failures = object.get("failures");
    if (failures != null && !failures.isObject()) {
      throw new IllegalArgumentException("key signatures failures must be an object");
    }
    return new KeySignaturesUploadResponse(failures == null ? null : failures.asObject(), value);
  }
}
