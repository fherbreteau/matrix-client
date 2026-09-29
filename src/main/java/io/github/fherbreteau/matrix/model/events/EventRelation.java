package io.github.fherbreteau.matrix.model.events;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Typed event relation fields and reply metadata, retaining raw JSON for relation extensions.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#forming-relationships-between-events">Matrix
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#definition-mrelates_to">Matrix
 *     relation definition</a>
 */
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
