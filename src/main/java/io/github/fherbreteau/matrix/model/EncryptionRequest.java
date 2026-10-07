package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;
import static java.util.Objects.requireNonNull;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/** Typed room-key backup upload data. */
public record EncryptionRequest(Map<RoomId, RoomKeyBackup> rooms) implements Serializable {
  private static final String MAC_KEY = "mac";
  private static final String EPHEMERAL_KEY = "ephemeral";
  private static final String CIPHERTEXT_KEY = "ciphertext";
  private static final String SESSION_DATA = "session_data";
  private static final String IS_VERIFIED = "is_verified";
  private static final String FORWARDED_COUNT = "forwarded_count";
  private static final String FIRST_MESSAGE_INDEX = "first_message_index";

  /**
   * Creates typed backup data for one of the backup route scopes.
   *
   * @param rooms all-room backup data
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3room_keyskeys">Matrix
   *     specification</a>
   */
  public EncryptionRequest {
    rooms = immutableMap(rooms);
  }

  @Override
  public JsonValue toJson() {
    JsonObject request = new JsonObject();
    request.put("rooms", ModelJson.toObject(rooms));
    return request;
  }

  /** Room Key backup. */
  public record RoomKeyBackup(Map<String, KeyBackupData> sessions) implements Serializable {

    /**
     * Creates room backup data.
     *
     * @param sessions A map of session IDs to key data.
     * @see <a
     *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3room_keyskeys">Matrix
     *     specification</a>
     */
    public RoomKeyBackup {
      sessions = immutableMap(sessions);
    }

    /**
     * Serialize the backup into a JsonValue.
     *
     * @return a JsonValue
     */
    public JsonValue toJson() {
      JsonObject request = new JsonObject();
      request.put("sessions", ModelJson.toObject(sessions));
      return request;
    }
  }

  /**
   * One encrypted Megolm session in a room-key backup.
   *
   * @param firstMessageIndex index of the first message
   * @param forwardedCount number of times the key was forwarded
   * @param verified whether the key is verified
   * @param sessionData encrypted algorithm-specific session data
   */
  public record KeyBackupData(
      long firstMessageIndex,
      long forwardedCount,
      boolean verified,
      EncryptedSessionData sessionData)
      implements Serializable {

    /**
     * Creates an encrypted backup session.
     *
     * @param firstMessageIndex index of the first message
     * @param forwardedCount number of times the key was forwarded
     * @param verified whether the key is verified
     * @param sessionData encrypted algorithm-specific session data
     */
    public KeyBackupData {
      requireNonNull(sessionData, "sessionData");
    }

    /**
     * Serialize the backup into a JsonValue.
     *
     * @return a JsonValue
     */
    public JsonValue toJson() {
      JsonObject request = new JsonObject();
      request.put(FIRST_MESSAGE_INDEX, firstMessageIndex);
      request.put(FORWARDED_COUNT, forwardedCount);
      request.put(IS_VERIFIED, verified);
      request.put(SESSION_DATA, sessionData.toJson());
      return request;
    }

    /**
     * Validate and parse a (/key/changes) response body.
     *
     * @param body the parsed response body
     * @return the validated key changes response
     * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
     *     versions} array of strings
     */
    public static KeyBackupData from(JsonValue body) {
      var request = ModelJson.object(body, "request");
      JsonObject sessionData = ModelJson.requiredObject(request, SESSION_DATA, "session");
      var encrypted = EncryptedSessionData.from(sessionData);
      return new KeyBackupData(
          ModelJson.requiredNumber(request, FIRST_MESSAGE_INDEX, SESSION_DATA),
          ModelJson.requiredNumber(request, FORWARDED_COUNT, SESSION_DATA),
          ModelJson.requiredBoolean(request, IS_VERIFIED),
          encrypted);
    }
  }

  /**
   * Encrypted data for the Megolm backup algorithm.
   *
   * @param ciphertext encrypted session key
   * @param ephemeral ephemeral public key
   * @param mac ciphertext authentication code
   */
  public record EncryptedSessionData(
      String ciphertext, String ephemeral, String mac, Map<String, Object> extraFields) {
    /**
     * Creates encrypted backup session data.
     *
     * @param ciphertext encrypted session key
     * @param ephemeral ephemeral public key
     * @param mac ciphertext authentication code
     * @param extraFields algorithm extension fields
     */
    public EncryptedSessionData {
      requireNonNull(ciphertext, CIPHERTEXT_KEY);
      requireNonNull(ephemeral, EPHEMERAL_KEY);
      requireNonNull(mac, MAC_KEY);
      extraFields = immutableMap(extraFields);
    }

    /** Creates standard encrypted session data without extension fields. */
    public EncryptedSessionData(String ciphertext, String ephemeral, String mac) {
      this(ciphertext, ephemeral, mac, null);
    }

    /**
     * Serialize the backup into a JsonValue.
     *
     * @return a JsonValue
     */
    public JsonValue toJson() {
      JsonObject request = new JsonObject();
      request.put(CIPHERTEXT_KEY, ciphertext);
      request.put(EPHEMERAL_KEY, ephemeral);
      request.put(MAC_KEY, mac);
      extraFields.forEach((key, value) -> request.put(key, ModelJson.toJsonValue(value)));
      return request;
    }

    static EncryptedSessionData from(JsonObject sessionData) {
      return new EncryptionRequest.EncryptedSessionData(
          ModelJson.requiredString(sessionData, CIPHERTEXT_KEY, SESSION_DATA),
          ModelJson.requiredString(sessionData, EPHEMERAL_KEY, SESSION_DATA),
          ModelJson.requiredString(sessionData, MAC_KEY, SESSION_DATA),
          ModelJson.optionalTypedMapExcept(sessionData, CIPHERTEXT_KEY, EPHEMERAL_KEY, MAC_KEY));
    }
  }
}
