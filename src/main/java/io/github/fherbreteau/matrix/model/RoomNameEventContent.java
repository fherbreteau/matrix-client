package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.name} state event, including the original JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroomname">Matrix
 *     specification</a>
 */
public record RoomNameEventContent(String name, JsonValue raw) implements EventContent {

  /** Parses the room name while retaining unknown fields. */
  public static RoomNameEventContent from(JsonValue content) {
    return new RoomNameEventContent(EventFields.string(content, "name"), content);
  }
}
