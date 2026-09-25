package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Typed or raw content for an event in a {@link RoomEvent} envelope.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#room-event-format">Matrix
 *     specification</a>
 */
public sealed interface EventContent
    permits MessageEventContent,
        MembershipEventContent,
        RoomNameEventContent,
        RoomTopicEventContent,
        PowerLevelsEventContent,
        CanonicalAliasEventContent,
        UnknownEventContent,
        RegisteredEventContent {

  /**
   * Returns the original JSON content.
   *
   * @return the raw content JSON
   */
  JsonValue raw();
}
