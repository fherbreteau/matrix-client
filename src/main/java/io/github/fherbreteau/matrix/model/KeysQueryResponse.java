package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parsed device-key query response.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysquery">Matrix
 *     specification</a>
 */
public record KeysQueryResponse(
    Map<UserId, Map<String, DeviceInformation>> deviceKeys,
    Map<String, Failure> failures,
    Map<UserId, CrossSigningKey> masterKeys,
    Map<UserId, CrossSigningKey> selfSigningKeys,
    Map<UserId, CrossSigningKey> userSigningKeys) {
  /**
   * Creates a parsed query response.
   *
   * @param deviceKeys returned devices by user
   * @param failures remote homeserver failures
   * @param masterKeys returned master keys
   * @param selfSigningKeys returned self-signing keys
   * @param userSigningKeys returned user-signing keys
   */
  public KeysQueryResponse {
    deviceKeys = ImmutableUtils.immutableMap(deviceKeys);
    failures = ImmutableUtils.immutableMap(failures);
    masterKeys = ImmutableUtils.immutableMap(masterKeys);
    selfSigningKeys = ImmutableUtils.immutableMap(selfSigningKeys);
    userSigningKeys = ImmutableUtils.immutableMap(userSigningKeys);
  }

  /** Remote homeserver failure information. */
  public record Failure(String errcode, String error) {}

  /**
   * Validate and parse a (/key/query) response body.
   *
   * @param body the parsed response body
   * @return the validated key query response
   * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
   *     versions} array of strings
   */
  public static KeysQueryResponse from(JsonValue body) {
    var response = ModelJson.object(body, "response");
    return new KeysQueryResponse(
        queryDeviceKeys(response.get("device_keys")),
        queryFailures(response.get("failures")),
        crossSigningKeys(response.get("master_keys")),
        crossSigningKeys(response.get("self_signing_keys")),
        crossSigningKeys(response.get("user_signing_keys")));
  }

  private static Map<UserId, Map<String, DeviceInformation>> queryDeviceKeys(JsonValue value) {
    if (value == null) {
      return Map.of();
    }
    JsonObject users = ModelJson.object(value, "device_keys");
    Map<UserId, Map<String, DeviceInformation>> result = new LinkedHashMap<>();
    users
        .entrySet()
        .forEach(
            userEntry -> {
              JsonObject devices = userEntry.getValue().asObject();
              Map<String, DeviceInformation> parsedDevices = new LinkedHashMap<>();
              devices
                  .entrySet()
                  .forEach(
                      deviceEntry ->
                          parsedDevices.put(
                              deviceEntry.getKey(), deviceInformation(deviceEntry.getValue())));
              result.put(UserId.of(userEntry.getKey()), parsedDevices);
            });
    return result;
  }

  private static DeviceInformation deviceInformation(JsonValue value) {
    JsonObject device = ModelJson.object(value, "device_keys");
    JsonObject unsigned =
        device.get("unsigned") == null
            ? null
            : ModelJson.object(device.get("unsigned"), "unsigned");
    JsonValue algorithms = device.get("algorithms");
    if (algorithms == null || !algorithms.isArray()) {
      throw new IllegalArgumentException("device_keys must contain algorithms");
    }
    JsonObject keys = ModelJson.requiredObject(device, "keys", "device_keys");
    JsonObject signatures = ModelJson.requiredObject(device, "signatures", "device_keys");
    return new DeviceInformation(
        ModelJson.strings(algorithms, "algorithms"),
        DeviceId.of(ModelJson.requiredString(device, "device_id", "device_keys")),
        ModelJson.optionalStringMap(keys),
        ModelJson.stringSignatures(signatures),
        DeviceInformation.UnsignedDeviceData.from(unsigned),
        UserId.of(ModelJson.requiredString(device, "user_id", "device_keys")));
  }

  private static Map<String, KeysQueryResponse.Failure> queryFailures(JsonValue value) {
    if (value == null) {
      return Map.of();
    }
    JsonObject failures = value.asObject();
    Map<String, KeysQueryResponse.Failure> result = new LinkedHashMap<>();
    failures
        .entrySet()
        .forEach(
            entry -> {
              JsonObject failure = entry.getValue().asObject();
              result.put(
                  entry.getKey(),
                  new KeysQueryResponse.Failure(
                      ModelJson.requiredString(failure, "errcode", "failure"),
                      ModelJson.requiredString(failure, "error", "failure")));
            });
    return result;
  }

  private static Map<UserId, CrossSigningKey> crossSigningKeys(JsonValue value) {
    if (value == null) {
      return null;
    }
    JsonObject users = value.asObject();
    Map<UserId, CrossSigningKey> result = new LinkedHashMap<>();
    users
        .entrySet()
        .forEach(
            entry -> {
              result.put(UserId.of(entry.getKey()), CrossSigningKey.from(entry.getValue()));
            });
    return result;
  }
}
