package io.github.fherbreteau.matrix.model.events;

import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.model.RoomEvent;

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
