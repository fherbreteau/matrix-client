package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * An event inside a room as delivered by the homeserver, following the {@code ClientEvent} format:
 * the server-assigned identity ({@code event_id}, {@code sender}, {@code origin_server_ts}, {@code
 * room_id}), the {@code state_key} for state events, and the raw {@code unsigned} data. The content
 * and unsigned data are kept as raw {@link JsonValue}s so unknown event types and unknown fields
 * are preserved.
 *
 * <p>To author state events (for {@code initial_state} or {@code sendStateEvent}), use {@link
 * StateEvent}, which carries only the fields a client may send.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/room-event-format">Matrix
 *     specification</a>
 */
public record RoomEvent(
    String eventId,
    String sender,
    String type,
    String stateKey,
    Long originServerTs,
    String roomId,
    JsonValue content,
    JsonValue unsigned) {

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
    JsonValue ts = obj.get("origin_server_ts");
    return new RoomEvent(
        stringValue(obj, "event_id"),
        stringValue(obj, "sender"),
        stringValue(obj, "type"),
        stringValue(obj, "state_key"),
        ts != null && ts.isNumber() ? ts.asLong() : null,
        stringValue(obj, "room_id"),
        obj.get("content"),
        obj.get("unsigned"));
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
