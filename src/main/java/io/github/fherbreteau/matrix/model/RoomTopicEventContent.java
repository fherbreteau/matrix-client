package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.topic} state event, including the original JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroomtopic">Matrix
 *     specification</a>
 */
public record RoomTopicEventContent(String topic, JsonValue raw) implements EventContent {

  /** Parses the room topic while retaining unknown fields. */
  public static RoomTopicEventContent from(JsonValue content) {
    return new RoomTopicEventContent(EventFields.string(content, "topic"), content);
  }
}
