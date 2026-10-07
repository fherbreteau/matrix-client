package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;
import java.util.Objects;

/**
 * Request to create or update a room-key backup version.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3room_keysversion">Matrix
 *     specification</a>
 */
public record RoomKeyBackupVersionRequest(
    String algorithm, BackupAuthData authData, String version) {
  /**
   * Creates a backup-version request.
   *
   * @param algorithm backup algorithm
   * @param authData algorithm-specific authentication data
   * @param version optional version for updates
   */
  public RoomKeyBackupVersionRequest {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(authData, "authData");
  }

  /** Authentication data for the Matrix room-key backup algorithm. */
  public record BackupAuthData(
      String publicKey,
      Map<UserId, Map<String, String>> signatures,
      Map<String, Object> extraFields) {
    private static final String SIGNATURES_KEY = "signatures";
    private static final String PUBLIC_KEY = "public_key";

    /**
     * Creates backup authentication data.
     *
     * @param publicKey backup public key
     * @param signatures optional signatures over the backup key
     * @param extraFields additional typed auth-data extension fields
     */
    public BackupAuthData {
      Objects.requireNonNull(publicKey, "publicKey");
      signatures = immutableMap(signatures);
      extraFields = immutableMap(extraFields);
    }

    /** Creates the standard authentication data without extensions. */
    public BackupAuthData(String publicKey, Map<UserId, Map<String, String>> signatures) {
      this(publicKey, signatures, null);
    }

    JsonValue toJson() {
      JsonObject body = new JsonObject().put(PUBLIC_KEY, publicKey);
      if (signatures != null) {
        body.put(SIGNATURES_KEY, ModelJson.toJsonValue(signatures));
      }
      if (extraFields != null) {
        extraFields.forEach((key, extra) -> body.put(key, ModelJson.toJsonValue(extra)));
      }
      return body;
    }

    static BackupAuthData from(JsonObject authData) {
      var publicKey = ModelJson.requiredString(authData, PUBLIC_KEY, "authData");
      var signatures = ModelJson.stringSignatures(authData.get(SIGNATURES_KEY));
      var extraFields = ModelJson.optionalTypedMapExcept(authData, PUBLIC_KEY, SIGNATURES_KEY);
      return new BackupAuthData(publicKey, signatures, extraFields);
    }
  }

  /**
   * Serialize the request.
   *
   * @param includeVersion flag indicating if the version should be included
   * @param version the new version to use
   * @return the JsonValue
   */
  public JsonValue toJson(boolean includeVersion, String... version) {
    JsonObject body =
        new JsonObject().put("algorithm", algorithm).put("auth_data", authData.toJson());
    if (includeVersion) {
      body.put("version", version.length == 0 ? this.version : version[0]);
    }
    return body;
  }
}
