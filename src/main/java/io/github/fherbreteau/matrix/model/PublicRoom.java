package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A public room as returned by the room directory. Known fields are exposed as accessors and the
 * raw chunk entry is preserved so unknown fields stay available for forward compatibility.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/get-matrixclientv3publicrooms">Matrix
 *     specification</a>
 */
public record PublicRoom(
    RoomId roomId,
    String name,
    String topic,
    String canonicalAlias,
    long numJoinedMembers,
    boolean guestCanJoin,
    boolean worldReadable,
    String avatarUrl,
    JsonValue raw) {

  /**
   * Parses a public-room chunk entry.
   *
   * @throws IllegalArgumentException if the value is not a JSON object with a room_id
   */
  public static PublicRoom from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("public room must be a JSON object");
    }
    JsonObject obj = value.asObject();
    JsonValue roomId = obj.get("room_id");
    if (roomId == null || !roomId.isString()) {
      throw new IllegalArgumentException("public room must contain a room_id");
    }
    return new PublicRoom(
        RoomId.of(roomId.asString()),
        stringValue(obj, "name"),
        stringValue(obj, "topic"),
        stringValue(obj, "canonical_alias"),
        longValue(obj, "num_joined_members"),
        boolValue(obj, "guest_can_join"),
        boolValue(obj, "world_readable"),
        stringValue(obj, "avatar_url"),
        value);
  }

  private static String stringValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isString() ? value.asString() : null;
  }

  private static long longValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isNumber() ? value.asLong() : 0L;
  }

  private static boolean boolValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isBoolean() && value.asBoolean();
  }
}
