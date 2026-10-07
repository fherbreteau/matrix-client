package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Backup write count and etag.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3room_keyskeys">Matrix
 *     specification</a>
 */
public record RoomKeyBackupWriteResponse(long count, String etag) {

  private static final String RESPONSE_LABEL = "response";

  /**
   * Validate and parse a (/key/changes) response body.
   *
   * @param body the parsed response body
   * @return the validated key changes response
   * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
   *     versions} array of strings
   */
  public static RoomKeyBackupWriteResponse from(JsonValue body) {
    var response = ModelJson.object(body, RESPONSE_LABEL);
    return new RoomKeyBackupWriteResponse(
        ModelJson.requiredNumber(response, "count", RESPONSE_LABEL),
        ModelJson.requiredString(response, "etag", RESPONSE_LABEL));
  }
}
