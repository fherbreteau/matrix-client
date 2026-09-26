package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/** A language-tagged or MIME-tagged text representation used by experimental {@code m.topic}. */
public record TopicTextRepresentation(String body, String mimeType, JsonValue raw) {

  static TopicTextRepresentation from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    String body = EventFields.string(value, EventFields.REPRESENTATION_BODY);
    String mimeType = EventFields.string(value, EventFields.MIME_TYPE);
    if (body == null
        || !value.asObject().has(EventFields.REPRESENTATION_BODY)
        || EventFields.hasWrongType(
            value.asObject(), EventFields.MIME_TYPE, EventFields.STRING_TYPE)) {
      return null;
    }
    return new TopicTextRepresentation(body, mimeType, value);
  }
}
