package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Identifies the newly created room-key backup version.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3room_keysversion">Matrix
 *     specification</a>
 */
public record RoomKeyBackupVersion(String version, JsonObject raw) {

  /**
   * Parses a backup-version creation response.
   *
   * @param value response JSON
   * @return opaque version and full response
   * @throws IllegalArgumentException if the version is missing
   */
  public static RoomKeyBackupVersion from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "room-key backup version response");
    return new RoomKeyBackupVersion(
        ModelJson.requiredString(object, "version", "room-key backup version response"), object);
  }
}
