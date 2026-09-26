package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/**
 * Content of an {@code m.room.power_levels} state event, preserving all fields as JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroompower_levels">Matrix
 *     specification</a>
 */
public record PowerLevelsEventContent(
    Long ban,
    Map<String, Long> events,
    Long eventsDefault,
    Long invite,
    Long kick,
    Map<String, Long> notifications,
    Long redact,
    Long stateDefault,
    Map<String, Long> users,
    Long usersDefault,
    JsonValue raw)
    implements EventContent {

  private static final String JSON_KEY_BAN = "ban";
  private static final String JSON_KEY_EVENTS = "events";
  private static final String JSON_KEY_EVENTS_DEFAULT = "events_default";
  private static final String JSON_KEY_INVITE = "invite";
  private static final String JSON_KEY_KICK = "kick";
  private static final String JSON_KEY_NOTIFICATIONS = "notifications";
  private static final String JSON_KEY_REDACT = "redact";
  private static final String JSON_KEY_STATE_DEFAULT = "state_default";
  private static final String JSON_KEY_USERS = "users";
  private static final String JSON_KEY_USERS_DEFAULT = "users_default";

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
    Long ban = EventFields.longValue(object, JSON_KEY_BAN);
    Long eventsDefault = EventFields.longValue(object, JSON_KEY_EVENTS_DEFAULT);
    Long invite = EventFields.longValue(object, JSON_KEY_INVITE);
    Long kick = EventFields.longValue(object, JSON_KEY_KICK);
    Long redact = EventFields.longValue(object, JSON_KEY_REDACT);
    Long stateDefault = EventFields.longValue(object, JSON_KEY_STATE_DEFAULT);
    Long usersDefault = EventFields.longValue(object, JSON_KEY_USERS_DEFAULT);
    if (hasWrongKnownFields(object)) {
      return null;
    }
    return new PowerLevelsEventContent(
        ban,
        EventFields.integerMap(object.get(JSON_KEY_EVENTS)),
        eventsDefault,
        invite,
        kick,
        EventFields.integerMap(object.get(JSON_KEY_NOTIFICATIONS)),
        redact,
        stateDefault,
        EventFields.integerMap(object.get(JSON_KEY_USERS)),
        usersDefault,
        content);
  }

  private static boolean hasWrongKnownFields(JsonObject object) {
    return EventFields.hasWrongType(object, JSON_KEY_BAN, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongIntegerMap(object, JSON_KEY_EVENTS)
        || EventFields.hasWrongType(object, JSON_KEY_EVENTS_DEFAULT, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongType(object, JSON_KEY_INVITE, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongType(object, JSON_KEY_KICK, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongType(object, JSON_KEY_NOTIFICATIONS, EventFields.OBJECT_TYPE)
        || EventFields.hasWrongType(object, JSON_KEY_REDACT, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongType(object, JSON_KEY_STATE_DEFAULT, EventFields.INTEGER_TYPE)
        || EventFields.hasWrongIntegerMap(object, JSON_KEY_USERS)
        || EventFields.hasWrongType(object, JSON_KEY_USERS_DEFAULT, EventFields.INTEGER_TYPE)
        || hasWrongNotificationLevels(object);
  }

  private static boolean hasWrongNotificationLevels(JsonObject content) {
    JsonValue notifications = content.get(JSON_KEY_NOTIFICATIONS);
    return EventFields.hasWrongIntegerObject(notifications);
  }
}
