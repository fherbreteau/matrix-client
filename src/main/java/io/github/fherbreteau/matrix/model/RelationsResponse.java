package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A page of child events related to a parent event, retaining unknown response fields.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#relationships-api">Matrix
 *     specification</a>
 */
public record RelationsResponse(
    List<RoomEvent> chunk,
    String nextBatch,
    String prevBatch,
    Integer recursionDepth,
    JsonValue raw) {

  /** Creates a response with an immutable list of events. */
  public RelationsResponse {
    chunk = List.copyOf(chunk);
  }

  /** Parses a relation-query response. */
  public static RelationsResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("relations response must be a JSON object");
    }
    JsonObject object = value.asObject();
    JsonValue chunkValue = object.get("chunk");
    if (chunkValue == null || !chunkValue.isArray()) {
      throw new IllegalArgumentException("relations response must contain chunk");
    }
    JsonArray array = chunkValue.asArray();
    List<RoomEvent> events = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      events.add(RoomEvent.from(array.get(index)));
    }
    return new RelationsResponse(
        events,
        stringValue(object, "next_batch"),
        stringValue(object, "prev_batch"),
        integerValue(object, "recursion_depth"),
        value);
  }

  /** Returns whether another page is available. */
  public boolean hasNextBatch() {
    return nextBatch != null;
  }

  private static String stringValue(JsonObject object, String key) {
    JsonValue value = object.get(key);
    return value != null && value.isString() ? value.asString() : null;
  }

  private static Integer integerValue(JsonObject object, String key) {
    JsonValue value = object.get(key);
    if (value == null || !value.isNumber()) {
      return null;
    }
    long depth = value.asLong();
    if (depth < 0 || depth > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("recursion_depth must be a non-negative integer");
    }
    return (int) depth;
  }
}
