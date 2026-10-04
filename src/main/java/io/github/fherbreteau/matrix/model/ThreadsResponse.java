package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A page of thread-root events in a room, retaining the complete raw response.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidthreads">Matrix
 *     specification</a>
 */
public record ThreadsResponse(List<RoomEvent> chunk, String nextBatch, JsonValue raw) {

  /** Creates a response with an immutable list of thread roots. */
  public ThreadsResponse {
    chunk = List.copyOf(chunk);
  }

  /** Parses a thread-list response. */
  public static ThreadsResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("threads response must be a JSON object");
    }
    JsonObject object = value.asObject();
    JsonValue chunkValue = object.get("chunk");
    if (chunkValue == null || !chunkValue.isArray()) {
      throw new IllegalArgumentException("threads response must contain chunk");
    }
    JsonArray array = chunkValue.asArray();
    List<RoomEvent> roots = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      roots.add(RoomEvent.from(array.get(index)));
    }
    JsonValue next = object.get("next_batch");
    return new ThreadsResponse(
        roots, next != null && next.isString() ? next.asString() : null, value);
  }

  /** Returns whether another page is available. */
  public boolean hasNextBatch() {
    return nextBatch != null;
  }
}
