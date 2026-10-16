package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/**
 * Request body for uploading cross-signing keys and optional UI-auth data.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysdevice_signingupload">Matrix
 *     specification</a>
 */
public record DeviceSigningUploadRequest(
    CrossSigningKey masterKey,
    CrossSigningKey selfSigningKey,
    CrossSigningKey userSigningKey,
    UiAuth auth)
    implements Serializable {

  /**
   * Create a request for uploading cross-signing keys without UI-auth data.
   *
   * @param masterKey the master signing key
   * @param selfSigningKey the self-signing key
   * @param userSigningKey the user-signing key
   */
  public DeviceSigningUploadRequest(
      CrossSigningKey masterKey, CrossSigningKey selfSigningKey, CrossSigningKey userSigningKey) {
    this(masterKey, selfSigningKey, userSigningKey, null);
  }

  @Override
  public JsonValue toJson() {
    JsonObject request = new JsonObject();
    if (auth != null) {
      request.put("auth", auth.toJson());
    }
    if (masterKey != null) {
      request.put("master_key", masterKey.toJson());
    }
    if (selfSigningKey != null) {
      request.put("self_signing_key", selfSigningKey.toJson());
    }
    if (userSigningKey != null) {
      request.put("user_signing_key", userSigningKey.toJson());
    }
    return request;
  }

  /** UI-auth response with typed credentials. */
  public record UiAuth(String type, String session, Map<String, String> others) {

    /**
     * Creates a UI-auth request.
     *
     * @param type The authentication type that the client is attempting to complete.
     * @param session The value of the session key given by the homeserver.
     * @param others Keys dependent on the login type
     */
    public UiAuth {
      others = immutableMap(others);
    }

    JsonValue toJson() {
      JsonObject request = new JsonObject();
      request.put("session", session);
      request.put("type", type);
      others.forEach(request::put);
      return request;
    }
  }
}
