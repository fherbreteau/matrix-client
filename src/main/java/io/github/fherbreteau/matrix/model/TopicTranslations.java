package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/** Typed representations in the experimental {@code m.topic.m.text} array. */
public record TopicTranslations(List<TopicTextRepresentation> text, JsonValue raw) {

  static TopicTranslations from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    JsonValue textValue = EventFields.field(value, EventFields.TEXT_REPRESENTATIONS);
    if (textValue == null) {
      return new TopicTranslations(List.of(), value);
    }
    if (!textValue.isArray()) {
      return null;
    }
    var text = new java.util.ArrayList<TopicTextRepresentation>();
    for (int i = 0; i < textValue.asArray().size(); i++) {
      TopicTextRepresentation representation =
          TopicTextRepresentation.from(textValue.asArray().get(i));
      if (representation == null) {
        return null;
      }
      text.add(representation);
    }
    return new TopicTranslations(List.copyOf(text), value);
  }
}
