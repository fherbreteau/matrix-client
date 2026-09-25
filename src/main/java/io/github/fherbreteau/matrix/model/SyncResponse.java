package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A complete response to `GET /_matrix/client/v3/sync`. The `next_batch` token is opaque: clients
 * should persist it and send it back as `since`, never interpret or modify it. Unknown response
 * fields are preserved in the raw value.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#syncing">Matrix specification</a>
 */
public record SyncResponse(
    String nextBatch,
    Map<String, SyncRoom> joinedRooms,
    Map<String, SyncRoom> invitedRooms,
    Map<String, SyncRoom> knockedRooms,
    Map<String, SyncRoom> leftRooms,
    JsonValue presence,
    JsonValue accountData,
    JsonValue toDevice,
    JsonValue deviceLists,
    JsonValue deviceOneTimeKeysCount,
    JsonValue raw) {

  /**
   * Parses a sync response.
   *
   * @throws IllegalArgumentException if the response is not a JSON object or lacks next_batch
   */
  public static SyncResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("sync response must be a JSON object");
    }
    JsonObject obj = value.asObject();
    JsonValue next = obj.get("next_batch");
    if (next == null || !next.isString()) {
      throw new IllegalArgumentException("sync response must contain next_batch");
    }
    JsonObject rooms = objectOrEmpty(obj.get("rooms"));
    validateStateAfter(rooms);
    return new SyncResponse(
        next.asString(),
        roomsMap(rooms.get("join")),
        roomsMap(rooms.get("invite")),
        roomsMap(rooms.get("knock")),
        roomsMap(rooms.get("leave")),
        obj.get("presence"),
        obj.get("account_data"),
        obj.get("to_device"),
        obj.get("device_lists"),
        obj.get("device_one_time_keys_count"),
        value);
  }

  private static void validateStateAfter(JsonObject rooms) {
    JsonValue joined = rooms.get("join");
    if (joined == null || !joined.isObject()) {
      return;
    }
    for (Map.Entry<String, JsonValue> roomEntry : joined.asObject().entrySet()) {
      JsonValue room = roomEntry.getValue();
      if (!room.isObject()) {
        continue;
      }
      JsonValue stateAfter = room.asObject().get("state_after");
      if (stateAfter != null && (!stateAfter.isObject() || !hasEventsArray(stateAfter))) {
        throw new IllegalArgumentException("state_after must contain an events array");
      }
    }
  }

  private static boolean hasEventsArray(JsonValue section) {
    JsonValue events = section.asObject().get("events");
    return events != null && events.isArray();
  }

  private static JsonObject objectOrEmpty(JsonValue value) {
    return value != null && value.isObject() ? value.asObject() : new JsonObject();
  }

  private static Map<String, SyncRoom> roomsMap(JsonValue value) {
    if (value == null || !value.isObject()) {
      return Map.of();
    }
    var rooms = new LinkedHashMap<String, SyncRoom>();
    for (Map.Entry<String, JsonValue> entry : value.asObject().entrySet()) {
      rooms.put(entry.getKey(), SyncRoom.from(entry.getValue()));
    }
    return Map.copyOf(rooms);
  }
}
