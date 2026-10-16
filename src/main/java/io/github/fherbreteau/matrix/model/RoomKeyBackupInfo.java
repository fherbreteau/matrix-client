package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;
import java.util.Objects;

/**
 * Room-key backup version metadata.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3room_keysversion">Matrix
 *     specification</a>
 */
public record RoomKeyBackupInfo(
    String algorithm,
    RoomKeyBackupVersionRequest.BackupAuthData authData,
    long count,
    String etag,
    String version,
    Map<String, Object> extraFields) {
  private static final String RESPONSE_LABEL = "response";
  private static final String ALGORITHM_KEY = "algorithm";
  private static final String VERSION_KEY = "version";

  /**
   * Creates backup-version metadata.
   *
   * @param algorithm backup algorithm
   * @param authData algorithm-specific authentication data
   * @param count stored session count
   * @param etag backup consistency token
   * @param version backup version identifier
   * @param extraFields additional typed extension fields
   */
  public RoomKeyBackupInfo {
    Objects.requireNonNull(algorithm, ALGORITHM_KEY);
    Objects.requireNonNull(authData, "authData");
    Objects.requireNonNull(etag, "etag");
    Objects.requireNonNull(version, VERSION_KEY);
    if (extraFields != null) {
      extraFields = immutableMap(extraFields);
    }
  }

  /** Creates metadata without extension fields. */
  public RoomKeyBackupInfo(
      String algorithm,
      RoomKeyBackupVersionRequest.BackupAuthData authData,
      long count,
      String etag,
      String version) {
    this(algorithm, authData, count, etag, version, null);
  }

  /**
   * Validate and parse a (/key/query) response body.
   *
   * @param body the parsed response body
   * @return the validated key query response
   * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
   *     versions} array of strings
   */
  public static RoomKeyBackupInfo from(JsonValue body) {
    var response = ModelJson.object(body, RESPONSE_LABEL);
    JsonObject authData = ModelJson.requiredObject(response, "auth_data", RESPONSE_LABEL);
    return new RoomKeyBackupInfo(
        ModelJson.requiredString(response, ALGORITHM_KEY, RESPONSE_LABEL),
        RoomKeyBackupVersionRequest.BackupAuthData.from(authData),
        ModelJson.requiredNumber(response, "count", RESPONSE_LABEL),
        ModelJson.requiredString(response, "etag", RESPONSE_LABEL),
        ModelJson.requiredString(response, VERSION_KEY, RESPONSE_LABEL),
        ModelJson.optionalTypedMapExcept(
            response, ALGORITHM_KEY, "auth_data", "count", "etag", VERSION_KEY));
  }
}
