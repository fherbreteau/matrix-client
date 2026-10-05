package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Acknowledgement containing the stored key count and backup etag.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3room_keyskeys">Matrix
 *     specification</a>
 */
public record RoomKeyBackupWriteResponse(long count, String etag, JsonObject raw) {

  /**
   * Parses a backup write response.
   *
   * @param value response JSON
   * @return count, etag, and complete response
   * @throws IllegalArgumentException if required fields are absent or malformed
   */
  public static RoomKeyBackupWriteResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "room-key backup write response");
    Long count = ModelJson.number(object, "count");
    if (count == null || count < 0) {
      throw new IllegalArgumentException("room-key backup write response must contain a count");
    }
    return new RoomKeyBackupWriteResponse(
        count, ModelJson.requiredString(object, "etag", "room-key backup write response"), object);
  }
}
