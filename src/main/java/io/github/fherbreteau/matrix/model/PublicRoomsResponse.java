package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * The public room directory as returned by {@code /publicRooms}. Unknown fields of the response are
 * ignored.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3publicrooms">Matrix
 *     specification</a>
 */
public record PublicRoomsResponse(
    List<PublicRoom> chunk, Long totalRoomCountEstimate, String nextBatch, String prevBatch) {

  /**
   * Parses a {@code /publicRooms} response.
   *
   * @throws IllegalArgumentException if the value is not a JSON object
   */
  public static PublicRoomsResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("public rooms response must be a JSON object");
    }
    JsonObject obj = value.asObject();
    var chunk = new ArrayList<PublicRoom>();
    JsonValue chunkValue = obj.get("chunk");
    if (chunkValue != null && chunkValue.isArray()) {
      JsonArray chunkArray = chunkValue.asArray();
      for (int i = 0; i < chunkArray.size(); i++) {
        chunk.add(PublicRoom.from(chunkArray.get(i)));
      }
    }
    JsonValue estimate = obj.get("total_room_count_estimate");
    JsonValue nextBatch = obj.get("next_batch");
    JsonValue prevBatch = obj.get("prev_batch");
    return new PublicRoomsResponse(
        List.copyOf(chunk),
        estimate != null && estimate.isNumber() ? estimate.asLong() : null,
        nextBatch != null && nextBatch.isString() ? nextBatch.asString() : null,
        prevBatch != null && prevBatch.isString() ? prevBatch.asString() : null);
  }
}
