package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.power_levels} state event, preserving all fields as JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroompower_levels">Matrix
 *     specification</a>
 */
public record PowerLevelsEventContent(
    Long ban,
    JsonValue events,
    Long eventsDefault,
    Long invite,
    Long kick,
    JsonValue notifications,
    Long redact,
    Long stateDefault,
    JsonValue users,
    Long usersDefault,
    JsonValue raw)
    implements EventContent {

  /**
   * Parses standard power-level fields while retaining unknown and future fields.
   *
   * @param content the raw event content
   * @return parsed power levels, or {@code null} when a known field has an invalid type
   */
  public static PowerLevelsEventContent from(JsonValue content) {
    if (content == null || !content.isObject()) {
      return null;
    }
    JsonObject object = content.asObject();
    Long ban = EventFields.longValue(object, "ban");
    Long eventsDefault = EventFields.longValue(object, "events_default");
    Long invite = EventFields.longValue(object, "invite");
    Long kick = EventFields.longValue(object, "kick");
    Long redact = EventFields.longValue(object, "redact");
    Long stateDefault = EventFields.longValue(object, "state_default");
    Long usersDefault = EventFields.longValue(object, "users_default");
    if (hasWrongKnownFields(object)) {
      return null;
    }
    return new PowerLevelsEventContent(
        ban,
        object.get("events"),
        eventsDefault,
        invite,
        kick,
        object.get("notifications"),
        redact,
        stateDefault,
        object.get("users"),
        usersDefault,
        content);
  }

  private static boolean hasWrongKnownFields(JsonObject object) {
    return EventFields.hasWrongType(object, "ban", "integer")
        || EventFields.hasWrongIntegerMap(object, "events")
        || EventFields.hasWrongType(object, "events_default", "integer")
        || EventFields.hasWrongType(object, "invite", "integer")
        || EventFields.hasWrongType(object, "kick", "integer")
        || EventFields.hasWrongType(object, "notifications", "object")
        || EventFields.hasWrongType(object, "redact", "integer")
        || EventFields.hasWrongType(object, "state_default", "integer")
        || EventFields.hasWrongIntegerMap(object, "users")
        || EventFields.hasWrongType(object, "users_default", "integer")
        || hasWrongNotificationLevels(object);
  }

  private static boolean hasWrongNotificationLevels(JsonObject object) {
    JsonValue notifications = object.get("notifications");
    return notifications != null
        && notifications.isObject()
        && EventFields.hasWrongType(notifications.asObject(), "room", "integer");
  }
}
