package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.model.EncryptionRequest.KeyBackupData;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Room-key backup response at whole-backup, room, or session scope.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keyskeys">Matrix
 *     specification</a>
 */
public record RoomKeyBackupKeysResponse(Map<RoomId, RoomSessions> rooms) {
  private static final String RESPONSE_LABEL = "response";

  /**
   * Creates a response for the requested backup scope.
   *
   * @param rooms sessions grouped by room ID
   */
  public RoomKeyBackupKeysResponse {
    rooms = immutableMap(rooms);
  }

  /** Sessions in one room. */
  public record RoomSessions(Map<String, EncryptionRequest.KeyBackupData> sessions) {
    /**
     * Copies the session map.
     *
     * @param sessions sessions keyed by ID
     */
    public RoomSessions {
      sessions = immutableMap(sessions);
    }

    /**
     * Validate and parse a (/key/changes) response body.
     *
     * @param body the parsed response body
     * @return the validated key changes response
     * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
     *     versions} array of strings
     */
    public static RoomSessions from(JsonValue body) {
      var response = ModelJson.object(body, RESPONSE_LABEL);
      var sessions = ModelJson.requiredObject(response, "sessions", RESPONSE_LABEL);
      return new RoomSessions(backupSessionMap(sessions));
    }

    private static Map<String, KeyBackupData> backupSessionMap(JsonObject sessions) {
      Map<String, KeyBackupData> result = new LinkedHashMap<>();
      sessions
          .entrySet()
          .forEach(
              entry -> {
                var sessionBackup = ModelJson.object(entry.getValue(), "session");
                result.put(entry.getKey(), KeyBackupData.from(sessionBackup));
              });
      return result;
    }
  }

  /**
   * Validate and parse a (/key/changes) response body.
   *
   * @param body the parsed response body
   * @return the validated key changes response
   * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
   *     versions} array of strings
   */
  public static RoomKeyBackupKeysResponse from(JsonValue body) {
    var response = ModelJson.object(body, RESPONSE_LABEL);
    var rooms = ModelJson.requiredObject(response, "rooms", RESPONSE_LABEL);
    return new RoomKeyBackupKeysResponse(backupRooms(rooms));
  }

  private static Map<RoomId, RoomSessions> backupRooms(JsonObject rooms) {
    Map<RoomId, RoomSessions> result = new LinkedHashMap<>();
    rooms
        .entrySet()
        .forEach(
            entry -> {
              var roomBackup = ModelJson.object(entry.getValue(), "room backup");
              var sessions = ModelJson.requiredObject(roomBackup, "sessions", "room backup");
              result.put(
                  RoomId.of(entry.getKey()),
                  new RoomKeyBackupKeysResponse.RoomSessions(
                      RoomSessions.backupSessionMap(sessions)));
            });
    return result;
  }
}
