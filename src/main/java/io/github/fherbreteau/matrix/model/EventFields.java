package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

final class EventFields {

  private EventFields() {}

  static JsonValue field(JsonValue content, String name) {
    return content != null && content.isObject() ? content.asObject().get(name) : null;
  }

  static String string(JsonValue content, String name) {
    JsonValue value = field(content, name);
    return value != null && value.isString() ? value.asString() : null;
  }
}
