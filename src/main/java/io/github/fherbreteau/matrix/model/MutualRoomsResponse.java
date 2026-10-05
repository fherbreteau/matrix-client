package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A page of rooms shared with another user.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mutual_rooms">Matrix
 *     specification</a>
 */
public record MutualRoomsResponse(
    long count, List<RoomId> joined, String nextBatch, JsonValue raw) {

  /** Copies room IDs into an immutable list. */
  public MutualRoomsResponse {
    joined = List.copyOf(joined);
  }

  /**
   * Parses a mutual-rooms response.
   *
   * @param value the response JSON
   * @return the parsed mutual-rooms page
   * @throws IllegalArgumentException if required fields are absent or malformed
   */
  public static MutualRoomsResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "mutual rooms response");
    Long count = ModelJson.number(object, "count");
    JsonValue joinedValue = object.get("joined");
    if (count == null || count < 0 || joinedValue == null || !joinedValue.isArray()) {
      throw new IllegalArgumentException("mutual rooms response is missing required fields");
    }
    JsonArray joinedArray = joinedValue.asArray();
    List<RoomId> joined = new ArrayList<>();
    for (int index = 0; index < joinedArray.size(); index++) {
      JsonValue roomId = joinedArray.get(index);
      if (!roomId.isString()) {
        throw new IllegalArgumentException("mutual rooms joined must contain room IDs");
      }
      joined.add(RoomId.of(roomId.asString()));
    }
    JsonValue nextBatch = object.get("next_batch");
    if (nextBatch != null && !nextBatch.isString()) {
      throw new IllegalArgumentException("mutual rooms next_batch must be a string");
    }
    return new MutualRoomsResponse(
        count, joined, nextBatch == null ? null : nextBatch.asString(), value);
  }

  /**
   * Returns whether another page is available.
   *
   * @return whether a continuation token is present
   */
  public boolean hasNextBatch() {
    return nextBatch != null;
  }
}
