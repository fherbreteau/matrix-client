package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A matching event and its relevance rank.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record SearchResult(
    RoomEvent result, Double rank, SearchResultContext context, JsonValue raw) {
  /**
   * Parses a search result.
   *
   * @param value result JSON
   * @return parsed result
   * @throws IllegalArgumentException if the required event is missing or malformed
   */
  public static SearchResult from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "search result");
    JsonValue event = object.get("result");
    if (event == null || !event.isObject()) {
      throw new IllegalArgumentException("search result must contain result event");
    }
    JsonObject eventObject = event.asObject();
    if (ModelJson.string(eventObject, "event_id") == null
        || ModelJson.string(eventObject, "sender") == null
        || ModelJson.string(eventObject, "type") == null
        || ModelJson.number(eventObject, "origin_server_ts") == null
        || ModelJson.string(eventObject, "room_id") == null
        || eventObject.get("content") == null
        || !eventObject.get("content").isObject()) {
      throw new IllegalArgumentException("search result event is missing required fields");
    }
    JsonValue context = object.get("context");
    JsonValue rankValue = object.get("rank");
    if (rankValue != null && !rankValue.isNumber()) {
      throw new IllegalArgumentException("search result rank must be a number");
    }
    if (context != null && !context.isObject()) {
      throw new IllegalArgumentException("search result context must be a JSON object");
    }
    return new SearchResult(
        RoomEvent.from(event),
        rankValue == null ? null : rankValue.asDouble(),
        context == null ? null : SearchResultContext.from(context),
        value);
  }
}
