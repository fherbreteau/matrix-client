package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A room entry in the response to `GET /_matrix/client/v3/sync`. The response keeps typed views of
 * the common event groups and the complete raw room object so new sync fields remain accessible.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#syncing">Matrix specification</a>
 */
public record SyncRoom(
    List<RoomEvent> timeline,
    List<RoomEvent> state,
    List<RoomEvent> stateAfter,
    List<RoomEvent> ephemeral,
    List<RoomEvent> accountData,
    List<RoomEvent> inviteState,
    List<RoomEvent> knockState,
    JsonValue raw) {

  /**
   * Parses a room sync section.
   *
   * @throws IllegalArgumentException if the room section is not a JSON object
   */
  public static SyncRoom from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("sync room section must be a JSON object");
    }
    JsonObject obj = value.asObject();
    return new SyncRoom(
        events(obj.get("timeline"), "events"),
        events(obj.get("state"), "events"),
        events(obj.get("state_after"), "events"),
        events(obj.get("ephemeral"), "events"),
        events(obj.get("account_data"), "events"),
        events(obj.get("invite_state"), "events"),
        events(obj.get("knock_state"), "events"),
        value);
  }

  private static List<RoomEvent> events(JsonValue section, String field) {
    if (section == null || !section.isObject()) {
      return List.of();
    }
    JsonValue chunk = section.asObject().get(field);
    if (chunk == null || !chunk.isArray()) {
      return List.of();
    }
    var events = new ArrayList<RoomEvent>();
    for (int i = 0; i < chunk.asArray().size(); i++) {
      events.add(RoomEvent.from(chunk.asArray().get(i)));
    }
    return List.copyOf(events);
  }
}
