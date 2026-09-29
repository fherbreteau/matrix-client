package io.github.fherbreteau.matrix.model.events;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * Typed message mentions, retaining unknown extension fields.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#user-and-room-mentions">Matrix
 *     specification</a>
 */
public record MessageMentions(Boolean room, List<String> userIds, JsonValue raw) {

  static MessageMentions from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    return new MessageMentions(
        EventFields.booleanValue(value, "room"),
        EventFields.stringList(EventFields.field(value, "user_ids")),
        value);
  }
}
