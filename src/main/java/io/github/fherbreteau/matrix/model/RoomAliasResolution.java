package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * The resolution of a room alias to a room identifier, as returned by {@code
 * /_matrix/client/v3/directory/room/{roomAlias}}. Unknown fields are ignored; the server list may
 * be empty.
 */
public record RoomAliasResolution(RoomId roomId, List<String> servers) {

  /**
   * Parses a room directory alias resolution response.
   *
   * @throws IllegalArgumentException if the value is not a JSON object
   */
  public static RoomAliasResolution from(JsonValue value) {
    JsonObject obj = value.asObject();
    JsonValue roomId = obj.get("room_id");
    if (roomId == null || !roomId.isString()) {
      throw new IllegalArgumentException("alias resolution must contain a room_id");
    }
    var servers = new ArrayList<String>();
    JsonValue serversValue = obj.get("servers");
    if (serversValue != null && serversValue.isArray()) {
      for (int i = 0; i < serversValue.asArray().size(); i++) {
        JsonValue server = serversValue.asArray().get(i);
        if (server.isString()) {
          servers.add(server.asString());
        }
      }
    }
    return new RoomAliasResolution(RoomId.of(roomId.asString()), List.copyOf(servers));
  }
}
