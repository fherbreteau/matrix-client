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

  private static final String EVENTS = "events";
  private static final String TIMELINE = "timeline";
  private static final String STATE = "state";
  private static final String STATE_AFTER = "state_after";
  private static final String EPHEMERAL = "ephemeral";
  private static final String ACCOUNT_DATA = "account_data";
  private static final String INVITE_STATE = "invite_state";
  private static final String KNOCK_STATE = "knock_state";

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
        events(obj.get(TIMELINE)),
        events(obj.get(STATE)),
        events(obj.get(STATE_AFTER)),
        events(obj.get(EPHEMERAL)),
        events(obj.get(ACCOUNT_DATA)),
        events(obj.get(INVITE_STATE)),
        events(obj.get(KNOCK_STATE)),
        value);
  }

  private static List<RoomEvent> events(JsonValue section) {
    if (section == null || !section.isObject()) {
      return List.of();
    }
    JsonValue chunk = section.asObject().get(EVENTS);
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
