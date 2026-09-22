package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * An event inside a room. The content is kept as a raw {@link JsonValue} so unknown event types and
 * unknown fields are preserved.
 */
public record RoomEvent(String eventId, String sender, String type, JsonValue content) {

  /**
   * Parses a room event from its JSON representation.
   *
   * @throws IllegalArgumentException if the value is not a JSON object
   */
  public static RoomEvent from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("event must be a JSON object");
    }
    JsonObject obj = value.asObject();
    return new RoomEvent(
        stringValue(obj, "event_id"),
        stringValue(obj, "sender"),
        stringValue(obj, "type"),
        obj.get("content"));
  }

  private static String stringValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isString() ? value.asString() : null;
  }
}
