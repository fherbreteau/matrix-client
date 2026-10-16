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
  private static final String DEVICE_KEYS_KEY = "device_keys";
  private static final String UNSIGNED_KEY = "unsigned";
  private static final String API_RESPONSE_KEY = "response";
  private static final String MASTER_KEYS_KEY = "master_keys";
  private static final String SELF_SIGNING_KEYS_KEY = "self_signing_keys";
  private static final String USER_SIGNING_KEYS_KEY = "user_signing_keys";
  private static final String FAILURES_KEY = "failures";
  private static final String ALGORITHMS_KEY = "algorithms";
  private static final String KEYS_KEY = "keys";
  private static final String SIGNATURES_KEY = "signatures";
  private static final String DEVICE_ID_KEY = "device_id";
  private static final String USER_ID_KEY = "user_id";
  private static final String ERROR_CODE_KEY = "errcode";
  private static final String ERROR_MESSAGE_KEY = "error";
  private static final String FAILURE_DESCRIPTION = "failure";
  private static final String MISSING_ALGORITHMS = "device_keys must contain algorithms";

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
    var response = ModelJson.object(body, API_RESPONSE_KEY);
    return new KeysQueryResponse(
        queryDeviceKeys(response.get(DEVICE_KEYS_KEY)),
        queryFailures(response.get(FAILURES_KEY)),
        crossSigningKeys(response.get(MASTER_KEYS_KEY)),
        crossSigningKeys(response.get(SELF_SIGNING_KEYS_KEY)),
        crossSigningKeys(response.get(USER_SIGNING_KEYS_KEY)));
  }

  private static Map<UserId, Map<String, DeviceInformation>> queryDeviceKeys(JsonValue value) {
    if (value == null) {
      return Map.of();
    }
    JsonObject users = ModelJson.object(value, DEVICE_KEYS_KEY);
    Map<UserId, Map<String, DeviceInformation>> result = new LinkedHashMap<>();
    users
        .entrySet()
        .forEach(
            userEntry -> {
              JsonObject devices = ModelJson.object(userEntry.getValue(), DEVICE_KEYS_KEY);
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
    JsonObject device = ModelJson.object(value, DEVICE_KEYS_KEY);
    JsonObject unsigned =
        device.get(UNSIGNED_KEY) == null
            ? null
            : ModelJson.object(device.get(UNSIGNED_KEY), UNSIGNED_KEY);
    JsonValue algorithms = device.get(ALGORITHMS_KEY);
    if (algorithms == null || !algorithms.isArray()) {
      throw new IllegalArgumentException(MISSING_ALGORITHMS);
    }
    JsonObject keys = ModelJson.requiredObject(device, KEYS_KEY, DEVICE_KEYS_KEY);
    JsonObject signatures = ModelJson.requiredObject(device, SIGNATURES_KEY, DEVICE_KEYS_KEY);
    return new DeviceInformation(
        ModelJson.strings(algorithms, ALGORITHMS_KEY),
        DeviceId.of(ModelJson.requiredString(device, DEVICE_ID_KEY, DEVICE_KEYS_KEY)),
        ModelJson.optionalStringMap(keys),
        ModelJson.stringSignatures(signatures),
        DeviceInformation.UnsignedDeviceData.from(unsigned),
        UserId.of(ModelJson.requiredString(device, USER_ID_KEY, DEVICE_KEYS_KEY)));
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
                      ModelJson.requiredString(failure, ERROR_CODE_KEY, FAILURE_DESCRIPTION),
                      ModelJson.requiredString(failure, ERROR_MESSAGE_KEY, FAILURE_DESCRIPTION)));
            });
    return result;
  }

  private static Map<UserId, CrossSigningKey> crossSigningKeys(JsonValue value) {
    if (value == null) {
      return Map.of();
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
