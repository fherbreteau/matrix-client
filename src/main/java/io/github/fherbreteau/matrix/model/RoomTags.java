package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The room tags associated with a user's room account data.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3useruseridroomsroomidtags">Matrix
 *     specification</a>
 */
public record RoomTags(Map<String, RoomTag> tags, JsonValue raw) {

  /** Copies room tags into an immutable insertion-ordered map. */
  public RoomTags {
    tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
  }

  /**
   * Parses a room-tag response.
   *
   * @param value the response JSON
   * @return the parsed room tags
   * @throws IllegalArgumentException if the response or its tags member is not a JSON object
   */
  public static RoomTags from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("room tags response must be a JSON object");
    }
    JsonValue tagsValue = value.asObject().get("tags");
    if (tagsValue == null || !tagsValue.isObject()) {
      throw new IllegalArgumentException("room tags response must contain a tags object");
    }
    Map<String, RoomTag> tags = new LinkedHashMap<>();
    for (Map.Entry<String, JsonValue> entry : tagsValue.asObject().entrySet()) {
      tags.put(entry.getKey(), RoomTag.from(entry.getValue()));
    }
    return new RoomTags(tags, value);
  }

  /**
   * Serializes the room tags.
   *
   * @return the room tags as JSON
   */
  public JsonObject toJson() {
    JsonObject tagValues = new JsonObject();
    tags.forEach((name, tag) -> tagValues.put(name, tag.toJson()));
    return new JsonObject().put("tags", tagValues);
  }
}
