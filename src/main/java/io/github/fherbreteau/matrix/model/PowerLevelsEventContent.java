package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.power_levels} state event, preserving all fields as JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroompower_levels">Matrix
 *     specification</a>
 */
public record PowerLevelsEventContent(JsonValue users, JsonValue events, JsonValue raw)
    implements EventContent {

  /** Parses common power-level sections while retaining unknown fields. */
  public static PowerLevelsEventContent from(JsonValue content) {
    JsonValue users = null;
    JsonValue events = null;
    if (content != null && content.isObject()) {
      JsonObject object = content.asObject();
      users = object.get("users");
      events = object.get("events");
    }
    return new PowerLevelsEventContent(users, events, content);
  }
}
