package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A server-side search response containing typed room-event results and raw unknown fields.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record SearchResponse(RoomEventsSearchResults roomEvents, JsonValue raw) {
  /**
   * Parses a server-side search response.
   *
   * @param value response JSON
   * @return parsed search response
   * @throws IllegalArgumentException if search_categories or its room_events result is missing
   */
  public static SearchResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "search response");
    JsonValue categories = object.get("search_categories");
    if (categories == null || !categories.isObject()) {
      throw new IllegalArgumentException("search response must contain search_categories");
    }
    JsonValue roomEvents = categories.asObject().get("room_events");
    if (roomEvents == null || !roomEvents.isObject()) {
      throw new IllegalArgumentException("search response must contain room_events results");
    }
    return new SearchResponse(RoomEventsSearchResults.from(roomEvents), value);
  }
}
