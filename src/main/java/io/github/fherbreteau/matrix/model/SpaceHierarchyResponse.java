package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A page of rooms returned while traversing a space hierarchy.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidhierarchy">Matrix
 *     specification</a>
 */
public record SpaceHierarchyResponse(
    List<SpaceHierarchyRoom> rooms, String nextBatch, JsonValue raw) {
  /** Copies hierarchy rooms to an immutable list. */
  public SpaceHierarchyResponse {
    rooms = List.copyOf(rooms);
  }

  /**
   * Parses a space hierarchy response.
   *
   * @param value response JSON
   * @return parsed response
   * @throws IllegalArgumentException if the response or required rooms field is malformed
   */
  public static SpaceHierarchyResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "space hierarchy response");
    JsonValue roomsValue = object.get("rooms");
    if (roomsValue == null || !roomsValue.isArray()) {
      throw new IllegalArgumentException("space hierarchy response must contain rooms");
    }
    JsonArray array = roomsValue.asArray();
    List<SpaceHierarchyRoom> rooms = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      rooms.add(SpaceHierarchyRoom.from(array.get(index)));
    }
    return new SpaceHierarchyResponse(rooms, ModelJson.string(object, "next_batch"), value);
  }
}
