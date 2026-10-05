package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Request nested user/key/signature mappings for signature upload.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keyssignaturesupload">Matrix
 *     specification</a>
 */
public record KeySignaturesUploadRequest(JsonObject payload) {

  /**
   * Wraps the signature upload body.
   *
   * @param value raw nested signature JSON
   * @return signature upload wrapper
   * @throws IllegalArgumentException if the value is not an object
   */
  public static KeySignaturesUploadRequest of(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("key signatures request must be a JSON object");
    }
    return new KeySignaturesUploadRequest(value.asObject());
  }

  /** Returns the original signature map. */
  public JsonObject toJson() {
    return payload;
  }
}
