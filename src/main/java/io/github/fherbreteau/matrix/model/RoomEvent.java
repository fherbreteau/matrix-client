package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * An event inside a room as delivered by the homeserver, following the {@code ClientEvent} format:
 * the server-assigned identity ({@code event_id}, {@code sender}, {@code origin_server_ts}, {@code
 * room_id}), the {@code state_key} for state events, and the raw {@code unsigned} data. The content
 * and unsigned data are kept as raw {@link JsonValue}s so unknown event types and unknown fields
 * are preserved. The complete envelope is available through {@link #raw()}, and callers can get a
 * typed content view through {@link #withTypedContent(EventRegistry)}.
 *
 * <p>To author state events (for {@code initial_state} or {@code sendStateEvent}), use {@link
 * StateEvent}, which carries only the fields a client may send.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#room-event-format">Matrix
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
    JsonValue unsigned,
    JsonValue raw) {

  private static final String EVENT_ID = "event_id";
  private static final String SENDER_FIELD = "sender";
  private static final String TYPE_FIELD = "type";
  private static final String STATE_KEY = "state_key";
  private static final String ORIGIN_SERVER_TS = "origin_server_ts";
  private static final String ROOM_ID = "room_id";
  private static final String CONTENT_FIELD = "content";
  private static final String UNSIGNED_FIELD = "unsigned";

  /** Creates a room event without a raw envelope, for application-created events. */
  public RoomEvent(
      String eventId,
      String sender,
      String type,
      String stateKey,
      Long originServerTs,
      String roomId,
      JsonValue content,
      JsonValue unsigned) {
    this(eventId, sender, type, stateKey, originServerTs, roomId, content, unsigned, null);
  }

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
    JsonValue ts = obj.get(ORIGIN_SERVER_TS);
    return new RoomEvent(
        stringValue(obj, EVENT_ID),
        stringValue(obj, SENDER_FIELD),
        stringValue(obj, TYPE_FIELD),
        stringValue(obj, STATE_KEY),
        ts != null && ts.isNumber() ? ts.asLong() : null,
        stringValue(obj, ROOM_ID),
        obj.get(CONTENT_FIELD),
        obj.get(UNSIGNED_FIELD),
        value);
  }

  /**
   * Returns whether this event is a state event, i.e. carries a state key.
   *
   * @return whether this event is a state event
   */
  public boolean isState() {
    return stateKey != null;
  }

  /**
   * Returns the raw JSON value for the complete event envelope.
   *
   * @return original event JSON, or {@code null} for manually constructed envelopes
   */
  @Override
  public JsonValue raw() {
    return raw;
  }

  /**
   * Returns this event paired with registered typed or fallback raw content.
   *
   * @param registry registry of built-in and application parsers
   * @return this event with typed content
   */
  public RegisteredRoomEvent withTypedContent(EventRegistry registry) {
    return new RegisteredRoomEvent(this, registry.parse(this));
  }

  private static String stringValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isString() ? value.asString() : null;
  }
}
