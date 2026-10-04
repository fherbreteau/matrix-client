package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Context events surrounding a matching search result.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record SearchResultContext(
    String start,
    String end,
    List<RoomEvent> eventsBefore,
    List<RoomEvent> eventsAfter,
    JsonValue profileInfo,
    JsonValue raw) {
  /** Copies event context lists to immutable lists. */
  public SearchResultContext {
    eventsBefore = List.copyOf(eventsBefore);
    eventsAfter = List.copyOf(eventsAfter);
  }

  /**
   * Parses event context attached to a search result.
   *
   * @param value event context JSON
   * @return parsed event context
   * @throws IllegalArgumentException if the value is not a JSON object
   */
  public static SearchResultContext from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "search result context");
    String start = ModelJson.string(object, "start");
    String end = ModelJson.string(object, "end");
    if ((object.get("start") != null && start == null)
        || (object.get("end") != null && end == null)) {
      throw new IllegalArgumentException("search result context pagination tokens must be strings");
    }
    JsonValue profileInfo = object.get("profile_info");
    if (profileInfo != null && !profileInfo.isObject()) {
      throw new IllegalArgumentException("search result context profile_info must be an object");
    }
    return new SearchResultContext(
        start,
        end,
        events(object.get("events_before"), "events_before"),
        events(object.get("events_after"), "events_after"),
        profileInfo,
        value);
  }

  private static List<RoomEvent> events(JsonValue value, String field) {
    if (value == null) {
      return List.of();
    }
    if (!value.isArray()) {
      throw new IllegalArgumentException(field + " must be an array");
    }
    List<RoomEvent> events = new ArrayList<>();
    for (int index = 0; index < value.asArray().size(); index++) {
      events.add(RoomEvent.from(value.asArray().get(index)));
    }
    return List.copyOf(events);
  }
}
