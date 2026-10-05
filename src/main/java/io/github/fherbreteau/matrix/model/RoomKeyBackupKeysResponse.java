package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Raw key backup data returned at whole-backup, room, or session scope.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keyskeys">Matrix
 *     specification</a>
 */
public record RoomKeyBackupKeysResponse(JsonObject payload) {

  /**
   * Parses a backup key response and retains all nested key fields.
   *
   * @param value the response JSON
   * @return the response wrapper
   * @throws IllegalArgumentException if the response is not an object
   */
  public static RoomKeyBackupKeysResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "room-key backup response");
    return new RoomKeyBackupKeysResponse(object);
  }

  /** Returns the raw response object. */
  public JsonObject toJson() {
    return payload;
  }
}
