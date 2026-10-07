package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableList;
import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;
import static java.util.Objects.requireNonNull;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;
import java.util.Map;

/**
 * Device information returned by a key query.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysquery">Matrix
 *     specification</a>
 */
public record DeviceInformation(
    List<String> algorithms,
    DeviceId deviceId,
    Map<String, String> keys,
    Map<UserId, Map<String, String>> signatures,
    UnsignedDeviceData unsigned,
    UserId userId)
    implements SignedObject {
  private static final String ALGORITHMS_KEY = "algorithms";
  private static final String DEVICE_ID_KEY = "device_id";
  private static final String KEYS_KEY = "keys";
  private static final String SIGNATURES_KEY = "signatures";
  private static final String UNSIGNED_KEY = "unsigned";
  private static final String USER_ID_KEY = "user_id";

  /**
   * Creates device information.
   *
   * @param algorithms supported algorithms
   * @param deviceId device identifier
   * @param keys device keys
   * @param signatures device key signatures
   * @param unsigned unsigned device metadata
   * @param userId owning user identifier
   */
  public DeviceInformation {
    algorithms = ModelJson.requireNotEmpty(immutableList(algorithms));
    deviceId = requireNonNull(deviceId);
    keys = ModelJson.requireNotEmpty(immutableMap(keys));
    signatures = immutableMap(signatures);
    userId = requireNonNull(userId);
  }

  /**
   * Creates device information.
   *
   * @param algorithms supported algorithms
   * @param deviceId device identifier
   * @param keys device keys
   * @param signatures device key signatures
   * @param userId owning user identifier
   */
  public DeviceInformation(
      List<String> algorithms,
      DeviceId deviceId,
      Map<String, String> keys,
      Map<UserId, Map<String, String>> signatures,
      UserId userId) {
    this(algorithms, deviceId, keys, signatures, null, userId);
  }

  @Override
  public JsonValue toJson() {
    JsonObject request = new JsonObject();
    request.put(ALGORITHMS_KEY, ModelJson.toArray(algorithms));
    request.put(DEVICE_ID_KEY, deviceId.value());
    request.put(KEYS_KEY, ModelJson.toObject(keys));
    request.put(SIGNATURES_KEY, ModelJson.toObject(signatures));
    if (unsigned != null) {
      request.put(UNSIGNED_KEY, unsigned.toJson());
    }
    request.put(USER_ID_KEY, userId.value());
    return request;
  }

  @Override
  public JsonValue toUnsignedJson() {
    JsonObject request = new JsonObject();
    request.put(ALGORITHMS_KEY, ModelJson.toArray(algorithms));
    request.put(DEVICE_ID_KEY, deviceId.value());
    request.put(KEYS_KEY, ModelJson.toObject(keys));
    request.put(USER_ID_KEY, userId.value());
    return request;
  }

  @Override
  public SignedObject withSignatures(Map<UserId, Map<String, String>> signatures) {
    return new DeviceInformation(
        this.algorithms, this.deviceId, this.keys, signatures, this.unsigned, this.userId);
  }

  /**
   * Validate and parse a cryptographic device.
   *
   * @param value the parsed device-key object
   * @return the validated cryptographic device
   * @throws DiscoveryException if the body is not a JSON object or does not contain a device key
   */
  public static DeviceInformation from(JsonValue value) {
    var json = ModelJson.object(value, "device_keys");
    JsonValue algorithms = json.get(ALGORITHMS_KEY);
    if (algorithms == null || !algorithms.isArray()) {
      throw new IllegalArgumentException("device_keys must contain algorithms");
    }
    JsonObject keys = ModelJson.requiredObject(json, KEYS_KEY, "device_keys");
    JsonObject signatures = ModelJson.requiredObject(json, SIGNATURES_KEY, "device_keys");
    JsonValue unsignedValue = json.get(UNSIGNED_KEY);
    JsonObject unsigned =
        unsignedValue == null ? null : ModelJson.object(unsignedValue, UNSIGNED_KEY);
    return new DeviceInformation(
        ModelJson.strings(algorithms, ALGORITHMS_KEY),
        DeviceId.of(ModelJson.requiredString(json, DEVICE_ID_KEY, "device_keys")),
        ModelJson.requiredStringMap(keys, "device_keys"),
        ModelJson.stringSignatures(signatures),
        UnsignedDeviceData.from(unsigned),
        UserId.of(ModelJson.requiredString(json, USER_ID_KEY, "device_keys")));
  }

  /** Unsigned device metadata. */
  public record UnsignedDeviceData(String deviceDisplayName) {

    JsonValue toJson() {
      JsonValue displayName = ModelJson.toJsonValue(deviceDisplayName);
      return new JsonObject(Map.of("device_display_name", displayName));
    }

    static UnsignedDeviceData from(JsonObject data) {
      if (data == null) {
        return null;
      }
      var displayName = ModelJson.optionalString(data, "device_display_name");
      return new DeviceInformation.UnsignedDeviceData(displayName);
    }
  }
}
