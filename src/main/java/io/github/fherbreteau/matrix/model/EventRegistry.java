package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry mapping event type strings to typed content parsers. Registration replaces a
 * parser for the same type; parse operations use a stable per-call parser snapshot. A parser that
 * throws or returns {@code null} falls back to {@link UnknownEventContent}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#room-event-format">Matrix
 *     specification</a>
 */
public final class EventRegistry {

  private final Map<String, EventContentParser<?>> parsers = new ConcurrentHashMap<>();

  /** Creates a registry containing parsers for common Matrix room events. */
  public EventRegistry() {
    register("m.room.message", MessageEventContent::from);
    register("m.room.member", MembershipEventContent::from);
    register("m.room.name", RoomNameEventContent::from);
    register("m.room.topic", RoomTopicEventContent::from);
    register("m.room.power_levels", PowerLevelsEventContent::from);
    register("m.room.canonical_alias", CanonicalAliasEventContent::from);
  }

  /**
   * Registers or replaces a parser for an event type.
   *
   * @param eventType event type handled by the parser
   * @param parser parser that converts content into an application type
   * @return this registry for chaining
   * @throws IllegalArgumentException if the event type or parser is null or blank
   */
  public EventRegistry register(String eventType, EventContentParser<?> parser) {
    if (eventType == null || eventType.isBlank()) {
      throw new IllegalArgumentException("event type is required");
    }
    if (parser == null) {
      throw new IllegalArgumentException("event parser is required");
    }
    parsers.put(eventType, parser);
    return this;
  }

  /**
   * Removes the parser for an event type. Built-in parsing is disabled for the removed type, and
   * its content is returned through the unknown raw fallback.
   *
   * @param eventType event type to remove
   * @return this registry for chaining
   */
  public EventRegistry unregister(String eventType) {
    if (eventType != null) {
      parsers.remove(eventType);
    }
    return this;
  }

  /**
   * Parses the event content according to the event type, falling back to raw content on unknown or
   * malformed event types.
   *
   * @param event the event envelope
   * @return typed content or an {@link UnknownEventContent}
   */
  public EventContent parse(RoomEvent event) {
    if (event == null || event.type() == null) {
      return new UnknownEventContent(
          event == null ? null : event.type(), event == null ? null : event.content());
    }
    if (event.content() == null || !event.content().isObject()) {
      return new UnknownEventContent(event.type(), event.content());
    }
    EventContentParser<?> parser = parsers.get(event.type());
    if (parser == null) {
      return new UnknownEventContent(event.type(), event.content());
    }
    try {
      Object parsed = parser.parse(event.content());
      if (parsed == null) {
        return new UnknownEventContent(event.type(), event.content());
      }
      return parsed instanceof EventContent content
          ? content
          : new RegisteredEventContent(event.type(), parsed, event.content());
    } catch (RuntimeException _) {
      return new UnknownEventContent(event.type(), event.content());
    }
  }

  /**
   * Parses a complete event and attaches the typed or raw content view.
   *
   * @param value complete event JSON
   * @return event envelope and typed content
   * @throws IllegalArgumentException if the event is not a JSON object
   */
  public RegisteredRoomEvent parseEvent(JsonValue value) {
    RoomEvent envelope = RoomEvent.from(value);
    return new RegisteredRoomEvent(envelope, parse(envelope));
  }
}
