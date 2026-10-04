package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * Criteria for searching room events on a homeserver.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record RoomEventsSearchCriteria(
    String searchTerm,
    List<String> keys,
    String orderBy,
    Boolean includeState,
    RoomEventFilter filter,
    SearchGrouping groupings,
    SearchEventContext eventContext) {
  /** Copies search keys to an immutable list and validates required search input. */
  public RoomEventsSearchCriteria {
    if (searchTerm == null) {
      throw new IllegalArgumentException("searchTerm is required");
    }
    keys = keys == null ? null : List.copyOf(keys);
    if (orderBy != null && !orderBy.equals("recent") && !orderBy.equals("rank")) {
      throw new IllegalArgumentException("orderBy must be recent or rank");
    }
  }

  /**
   * Serializes the criteria using the room_events category field names.
   *
   * @return search criteria JSON
   */
  public JsonValue toJson() {
    JsonObject object = new JsonObject();
    object.put("search_term", searchTerm);
    if (keys != null) {
      object.put("keys", ModelJson.toArray(keys));
    }
    if (orderBy != null) {
      object.put("order_by", orderBy);
    }
    if (includeState != null) {
      object.put("include_state", includeState);
    }
    if (filter != null) {
      object.put("filter", filter.toJson());
    }
    if (groupings != null) {
      object.put("groupings", groupings.toJson());
    }
    if (eventContext != null) {
      object.put("event_context", eventContext.toJson());
    }
    return object;
  }
}
