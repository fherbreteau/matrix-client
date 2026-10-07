package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/**
 * Request body for uploading key signatures.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keyssignaturesupload">Matrix
 *     specification</a>
 */
public record KeySignaturesUploadRequest(Map<UserId, Map<String, SignedObject>> signatures)
    implements Serializable {
  /**
   * Creates a signature upload request.
   *
   * @param signatures mappings from users to signed key objects
   */
  public KeySignaturesUploadRequest {
    signatures = immutableMap(signatures);
  }

  @Override
  public JsonValue toJson() {
    return ModelJson.toObject(signatures);
  }
}
