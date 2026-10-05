package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Request body for creating or updating a room-key backup version.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3room_keysversion">Matrix
 *     specification</a>
 */
public record RoomKeyBackupVersionRequest(JsonObject payload) {

  /**
   * Wraps a version request object while retaining its algorithm-specific fields.
   *
   * @param value raw request JSON
   * @return typed wrapper
   * @throws IllegalArgumentException if required fields are missing or malformed
   */
  public static RoomKeyBackupVersionRequest of(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("room-key backup version request must be an object");
    }
    JsonObject object = value.asObject();
    if (ModelJson.string(object, "algorithm") == null
        || object.get("auth_data") == null
        || !object.get("auth_data").isObject()) {
      throw new IllegalArgumentException(
          "room-key backup version request is missing required fields");
    }
    JsonValue version = object.get("version");
    if (version != null && !version.isString()) {
      throw new IllegalArgumentException("room-key backup version must be a string");
    }
    return new RoomKeyBackupVersionRequest(object);
  }

  /**
   * Creates a backup-version creation request.
   *
   * @param algorithm backup algorithm
   * @param authData raw algorithm-specific authentication data
   * @return request body
   */
  public static RoomKeyBackupVersionRequest create(String algorithm, JsonValue authData) {
    if (algorithm == null || algorithm.isBlank() || authData == null || !authData.isObject()) {
      throw new IllegalArgumentException("backup algorithm and auth data are required");
    }
    return new RoomKeyBackupVersionRequest(
        new JsonObject().put("algorithm", algorithm).put("auth_data", authData));
  }

  /** Returns the raw request body. */
  public JsonObject toJson() {
    return payload;
  }

  /** Returns algorithm-specific authentication data. */
  public JsonObject authData() {
    return payload.get("auth_data").asObject();
  }

  /** Returns an optional body version. */
  public String version() {
    JsonValue value = payload.get("version");
    return value == null ? null : value.asString();
  }
}
