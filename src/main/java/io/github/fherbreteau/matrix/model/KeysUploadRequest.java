package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonString;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;
import java.util.Objects;

/**
 * Request for uploading device identity, one-time, and fallback keys.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysupload">Matrix
 *     specification</a>
 */
public record KeysUploadRequest(
    DeviceInformation deviceKeys,
    Map<String, KeyValue> oneTimeKeys,
    Map<String, KeyValue> fallbackKeys) {

  private static final String SIGNATURE_KEY = "signatures";

  /**
   * Creates an upload request with defensive copies of optional key maps.
   *
   * @param deviceKeys optional device identity data
   * @param oneTimeKeys optional one-time keys
   * @param fallbackKeys optional fallback keys
   */
  public KeysUploadRequest {
    oneTimeKeys = immutableMap(oneTimeKeys);
    fallbackKeys = immutableMap(fallbackKeys);
  }

  /**
   * Serialize the request into a JsonValue.
   *
   * @return the request body as JSON
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysupload">Matrix
   *     specification</a>
   */
  public JsonValue toJson() {
    JsonObject request = new JsonObject();
    // Serialize the Device Keys
    if (deviceKeys != null) {
      request.put("device_keys", deviceKeys.toJson());
    }
    if (!fallbackKeys.isEmpty()) {
      request.put("fallback_keys", ModelJson.toJsonValue(fallbackKeys));
    }
    if (!oneTimeKeys.isEmpty()) {
      request.put("one_time_keys", ModelJson.toJsonValue(oneTimeKeys));
    }
    return request;
  }

  /** An unpublished one-time or fallback key. */
  public sealed interface KeyValue extends Serializable permits PlainKey, SignedKey {}

  /** Plain key material. */
  public record PlainKey(String key) implements KeyValue {
    /**
     * Creates plain key data.
     *
     * @param key public key material
     */
    public PlainKey {
      Objects.requireNonNull(key, "key");
    }

    @Override
    public JsonValue toJson() {
      return JsonString.of(key);
    }
  }

  /** A signed one-time or fallback key value. */
  public record SignedKey(String key, Map<UserId, Map<String, String>> signatures, Boolean fallback)
      implements KeyValue, SignedObject {
    /**
     * Creates a signed key material.
     *
     * @param key public key material
     * @param signatures signatures by user and key ID
     */
    public SignedKey {
      signatures = immutableMap(signatures);
    }

    @Override
    public JsonValue toJson() {
      JsonObject request = new JsonObject();
      request.put(SIGNATURE_KEY, ModelJson.toJsonValue(signatures));
      if (fallback != null) {
        request.put("fallback", fallback);
      }
      request.put("key", key);
      return request;
    }

    @Override
    public JsonValue toUnsignedJson() {
      JsonObject request = new JsonObject();
      if (fallback != null) {
        request.put("fallback", fallback);
      }
      request.put("key", key);
      return request;
    }

    @Override
    public SignedKey withSignatures(Map<UserId, Map<String, String>> signatures) {
      return new SignedKey(this.key, signatures, this.fallback);
    }
  }
}
