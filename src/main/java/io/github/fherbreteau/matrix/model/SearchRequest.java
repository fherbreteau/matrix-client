package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A server-side full-text search request.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record SearchRequest(RoomEventsSearchCriteria roomEvents, String nextBatch) {
  /**
   * Serializes the request body.
   *
   * @return search request JSON
   */
  public JsonValue toJson() {
    return new JsonObject()
        .put("search_categories", new JsonObject().put("room_events", roomEvents.toJson()));
  }

  /**
   * Serializes the optional pagination token as a query parameter.
   *
   * @return query parameters, preserving the pagination token as opaque data
   */
  public JsonObject toQuery() {
    JsonObject query = new JsonObject();
    if (nextBatch != null) {
      query.put("next_batch", nextBatch);
    }
    return query;
  }
}
