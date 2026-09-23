package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A page of room history as returned by {@code /rooms/{roomId}/messages}. The chunk is a list of
 * events whose order is defined by the homeserver; callers must not assume any specific ordering
 * and should use the {@code start}/{@code end} tokens to navigate. Unknown event types and fields
 * are preserved in the parsed events.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3roomsroomidmessages">Matrix
 *     specification</a>
 */
public record RoomMessagesPage(
    List<RoomEvent> chunk, String start, String end, List<RoomEvent> state) {

  /**
   * Parses a {@code /messages} response.
   *
   * @throws IllegalArgumentException if the value is not a JSON object or does not carry a {@code
   *     start} token
   */
  public static RoomMessagesPage from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("messages response must be a JSON object");
    }
    JsonObject obj = value.asObject();
    JsonValue start = obj.get("start");
    if (start == null || !start.isString()) {
      throw new IllegalArgumentException("messages response must contain a start token");
    }
    var chunk = new ArrayList<RoomEvent>();
    JsonValue chunkValue = obj.get("chunk");
    if (chunkValue != null && chunkValue.isArray()) {
      JsonArray chunkArray = chunkValue.asArray();
      for (int i = 0; i < chunkArray.size(); i++) {
        chunk.add(RoomEvent.from(chunkArray.get(i)));
      }
    }
    JsonValue end = obj.get("end");
    var state = new ArrayList<RoomEvent>();
    JsonValue stateValue = obj.get("state");
    if (stateValue != null && stateValue.isArray()) {
      JsonArray stateArray = stateValue.asArray();
      for (int i = 0; i < stateArray.size(); i++) {
        state.add(RoomEvent.from(stateArray.get(i)));
      }
    }
    return new RoomMessagesPage(
        List.copyOf(chunk),
        start.asString(),
        end != null && end.isString() ? end.asString() : null,
        state);
  }

  /** Returns whether another page can be requested from the returned {@code end} token. */
  public boolean hasEnd() {
    return end != null;
  }
}
