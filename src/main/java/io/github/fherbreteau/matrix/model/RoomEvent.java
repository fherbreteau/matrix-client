package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * An event inside a room as delivered by the homeserver, carrying the server-assigned identity
 * ({@code event_id}, {@code sender}) and, for state events, the {@code state_key}. The content is
 * kept as a raw {@link JsonValue} so unknown event types and unknown fields are preserved.
 *
 * <p>To author state events (for {@code initial_state} or {@code sendStateEvent}), use {@link
 * StateEvent}, which carries only the fields a client may send.
 */
public record RoomEvent(
    String eventId, String sender, String type, String stateKey, JsonValue content) {

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
        stringValue(obj, "state_key"),
        obj.get("content"));
  }

  /** Returns whether this event is a state event, i.e. carries a state key. */
  public boolean isState() {
    return stateKey != null;
  }

  private static String stringValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isString() ? value.asString() : null;
  }
}
