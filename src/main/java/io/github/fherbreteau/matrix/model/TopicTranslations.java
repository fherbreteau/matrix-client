package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/** Typed experimental {@code m.topic} topic translations with raw JSON retained. */
public record TopicTranslations(JsonValue text, JsonValue raw) {

  static TopicTranslations from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    if (EventFields.hasWrongTextRepresentations(value)) {
      return null;
    }
    JsonValue text = EventFields.field(value, "m.text");
    return new TopicTranslations(text, value);
  }
}
