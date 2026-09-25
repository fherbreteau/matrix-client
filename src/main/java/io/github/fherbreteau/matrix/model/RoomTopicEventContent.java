package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.topic} state event, including the original JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroomtopic">Matrix
 *     specification</a>
 */
public record RoomTopicEventContent(
    String topic, TopicTranslations topicTranslations, JsonValue raw) implements EventContent {

  /** Parses the room topic while retaining unknown fields. */
  public static RoomTopicEventContent from(JsonValue content) {
    if (content == null || !content.isObject()) {
      return null;
    }
    String topic = EventFields.string(content, "topic");
    if (topic == null || EventFields.hasWrongType(content.asObject(), "topic", "string")) {
      return null;
    }
    JsonValue translations = EventFields.field(content, "m.topic");
    if (translations != null && !translations.isObject()) {
      return null;
    }
    return new RoomTopicEventContent(topic, TopicTranslations.from(translations), content);
  }
}
