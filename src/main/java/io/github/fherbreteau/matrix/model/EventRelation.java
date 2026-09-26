package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/** Typed event relation fields and reply metadata, retaining raw JSON for relation extensions. */
public record EventRelation(
    String eventId, String relationType, String inReplyToEventId, JsonValue raw) {

  static EventRelation from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    JsonValue reply = EventFields.field(value, "m.in_reply_to");
    return new EventRelation(
        EventFields.string(value, "event_id"),
        EventFields.string(value, "rel_type"),
        EventFields.string(reply, "event_id"),
        value);
  }
}
