package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Raw room-key backup version metadata and algorithm-dependent auth data.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keysversion">Matrix
 *     specification</a>
 */
public record RoomKeyBackupInfo(JsonObject payload) {

  /**
   * Parses backup metadata and validates its required properties.
   *
   * @param value response JSON
   * @return backup metadata
   * @throws IllegalArgumentException if required fields are absent or malformed
   */
  public static RoomKeyBackupInfo from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "room-key backup info");
    if (ModelJson.string(object, "algorithm") == null
        || object.get("auth_data") == null
        || !object.get("auth_data").isObject()
        || ModelJson.number(object, "count") == null
        || ModelJson.string(object, "etag") == null
        || ModelJson.string(object, "version") == null) {
      throw new IllegalArgumentException("room-key backup info is missing required fields");
    }
    return new RoomKeyBackupInfo(object);
  }

  /** Returns the backup algorithm. */
  public String algorithm() {
    return payload.get("algorithm").asString();
  }

  /** Returns algorithm-specific authentication data without normalizing it. */
  public JsonObject authData() {
    return payload.get("auth_data").asObject();
  }

  /** Returns the server-reported backed-up key count. */
  public long count() {
    return payload.get("count").asLong();
  }

  /** Returns the opaque etag used for backup consistency. */
  public String etag() {
    return payload.get("etag").asString();
  }

  /** Returns the opaque version identifier. */
  public String version() {
    return payload.get("version").asString();
  }

  /** Returns the complete response including unrecognized fields. */
  public JsonObject raw() {
    return payload;
  }
}
