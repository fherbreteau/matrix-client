package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Newly created room-key backup version identifier.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3room_keysversion">Matrix
 *     specification</a>
 */
public record RoomKeyBackupVersion(String version) {

  /**
   * Parse a newly created backup version response.
   *
   * @param response the parsed response body
   * @return the validated backup version
   * @throws IllegalArgumentException if the response is malformed
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3room_keysversion">Matrix
   *     specification</a>
   */
  public static RoomKeyBackupVersion from(JsonValue response) {
    var body = ModelJson.object(response, "response");
    return new RoomKeyBackupVersion(ModelJson.requiredString(body, "version", "response"));
  }
}
