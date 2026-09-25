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

  private static final String BAN = "ban";
  private static final String EVENTS = "events";
  private static final String EVENTS_DEFAULT = "events_default";
  private static final String INVITE = "invite";
  private static final String KICK = "kick";
  private static final String NOTIFICATIONS = "notifications";
  private static final String ROOM = "room";
  private static final String REDACT = "redact";
  private static final String STATE_DEFAULT = "state_default";
  private static final String USERS = "users";
  private static final String USERS_DEFAULT = "users_default";

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
    Long ban = EventFields.longValue(object, BAN);
    Long eventsDefault = EventFields.longValue(object, EVENTS_DEFAULT);
    Long invite = EventFields.longValue(object, INVITE);
    Long kick = EventFields.longValue(object, KICK);
    Long redact = EventFields.longValue(object, REDACT);
    Long stateDefault = EventFields.longValue(object, STATE_DEFAULT);
    Long usersDefault = EventFields.longValue(object, USERS_DEFAULT);
    if (hasWrongKnownFields(object)) {
      return null;
    }
    return new PowerLevelsEventContent(
        ban,
        object.get(EVENTS),
        eventsDefault,
        invite,
        kick,
        object.get(NOTIFICATIONS),
        redact,
        stateDefault,
        object.get(USERS),
        usersDefault,
        content);
  }

  private static boolean hasWrongKnownFields(JsonObject object) {
    return EventFields.hasWrongType(object, BAN, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongIntegerMap(object, EVENTS)
        || EventFields.hasWrongType(object, EVENTS_DEFAULT, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongType(object, INVITE, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongType(object, KICK, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongType(object, NOTIFICATIONS, EventFields.OBJECT_TYPE)
        || EventFields.hasWrongType(object, REDACT, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongType(object, STATE_DEFAULT, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongIntegerMap(object, USERS)
        || EventFields.hasWrongType(object, USERS_DEFAULT, EventFields.INTEGER_TYPE)
        || hasWrongNotificationLevels(object);
  }

  private static boolean hasWrongNotificationLevels(JsonObject object) {
    JsonValue notifications = object.get(NOTIFICATIONS);
    return notifications != null
        && notifications.isObject()
        && EventFields.hasWrongType(notifications.asObject(), ROOM, EventFields.INTEGER_TYPE);
  }
}
