package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/** Typed message mentions, retaining unknown extension fields. */
public record MessageMentions(Boolean room, JsonValue userIds, JsonValue raw) {

  static MessageMentions from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    return new MessageMentions(
        EventFields.booleanValue(value, "room"), EventFields.field(value, "user_ids"), value);
  }
}
