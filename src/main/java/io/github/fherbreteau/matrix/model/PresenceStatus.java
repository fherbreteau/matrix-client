package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * The presence status of a user as returned by {@code GET
 * /_matrix/client/v3/presence/{userId}/status}. Unknown fields of the response are preserved in the
 * raw value.
 */
public record PresenceStatus(
    Presence presence,
    String statusMessage,
    Long lastActiveAgo,
    Boolean currentlyActive,
    JsonValue raw) {

  /**
   * Parses a presence-status response.
   *
   * @throws IllegalArgumentException if the value is not a JSON object or does not carry a
   *     spec-defined {@code presence}
   */
  public static PresenceStatus from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("presence status must be a JSON object");
    }
    JsonObject obj = value.asObject();
    JsonValue presence = obj.get("presence");
    if (presence == null || !presence.isString()) {
      throw new IllegalArgumentException("presence status must contain a presence");
    }
    return new PresenceStatus(
        Presence.from(presence.asString()),
        stringValue(obj, "status_msg"),
        longValue(obj, "last_active_ago"),
        boolValue(obj, "currently_active"),
        value);
  }

  /**
   * Serializes a presence update as the body of {@code PUT
   * /_matrix/client/v3/presence/{userId}/status}: the presence is required and the optional status
   * message is omitted when unset.
   *
   * @return the presence-update body
   */
  public JsonValue toUpdate() {
    JsonObject body = new JsonObject().put("presence", presence.value());
    if (statusMessage != null) {
      body.put("status_msg", statusMessage);
    }
    return body;
  }

  /**
   * Creates a presence update from a presence and an optional status message.
   *
   * @param presence the presence to set
   * @param statusMessage the optional status message
   * @return a presence update body
   */
  public static PresenceStatus of(Presence presence, String statusMessage) {
    return new PresenceStatus(presence, statusMessage, null, null, new JsonObject());
  }

  private static String stringValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isString() ? value.asString() : null;
  }

  private static Long longValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isNumber() ? value.asLong() : null;
  }

  private static Boolean boolValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isBoolean() ? value.asBoolean() : null;
  }
}
