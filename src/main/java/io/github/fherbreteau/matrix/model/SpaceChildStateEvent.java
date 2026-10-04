package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * An event in the {@code children_state} of a space hierarchy room.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidhierarchy">Matrix
 *     specification</a>
 */
public record SpaceChildStateEvent(
    JsonValue content,
    long originServerTs,
    UserId sender,
    String stateKey,
    String type,
    JsonValue raw) {
  private static final String DESCRIPTION = "space child state event";

  /**
   * Parses a stripped child state event.
   *
   * @param value child state event JSON
   * @return parsed child state event
   * @throws IllegalArgumentException if required fields are absent or malformed
   */
  public static SpaceChildStateEvent from(JsonValue value) {
    JsonObject object = ModelJson.object(value, DESCRIPTION);
    JsonValue content = object.get("content");
    Long timestamp = ModelJson.number(object, "origin_server_ts");
    String sender = ModelJson.requiredString(object, "sender", DESCRIPTION);
    String stateKey = ModelJson.requiredString(object, "state_key", DESCRIPTION);
    String type = ModelJson.requiredString(object, "type", DESCRIPTION);
    if (content == null || !content.isObject() || timestamp == null) {
      throw new IllegalArgumentException(DESCRIPTION + " is missing required fields");
    }
    return new SpaceChildStateEvent(content, timestamp, UserId.of(sender), stateKey, type, value);
  }
}
