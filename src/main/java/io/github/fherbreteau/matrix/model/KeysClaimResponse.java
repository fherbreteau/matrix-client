package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;
import static java.util.Objects.requireNonNull;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parsed claimed one-time key response.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysclaim">Matrix
 *     specification</a>
 */
public record KeysClaimResponse(
    Map<UserId, Map<String, Map<String, ClaimedKey>>> oneTimeKeys, Map<String, Failure> failures) {
  /**
   * Creates a parsed key-claim response.
   *
   * @param oneTimeKeys claimed keys by user, device, and key ID
   * @param failures remote homeserver failures
   */
  public KeysClaimResponse {
    oneTimeKeys = ModelJson.requireNotEmpty(immutableMap(oneTimeKeys));
    failures = immutableMap(failures);
  }

  /** A claimed one-time or fallback key. */
  public sealed interface ClaimedKey permits PlainKey, SignedKey {}

  /** Plain key material. */
  public record PlainKey(String key) implements ClaimedKey {
    /**
     * Creates plain key data.
     *
     * @param key public key material
     */
    public PlainKey {
      requireNonNull(key, "key");
    }
  }

  /** Signed key material. */
  public record SignedKey(String key, Map<UserId, Map<String, String>> signatures)
      implements ClaimedKey {
    /**
     * Creates a signed claimed key.
     *
     * @param key public key material
     * @param signatures signatures by user and key ID
     */
    public SignedKey {
      key = requireNonNull(key);
      signatures = ModelJson.requireNotEmpty(immutableMap(signatures));
    }
  }

  /** Remote homeserver failure information. */
  public record Failure(String errcode, String error) {}

  /**
   * Validate and parse a (/key/claims) response body.
   *
   * @param body the parsed response body
   * @return the validated key claims response
   * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
   *     versions} array of strings
   */
  public static KeysClaimResponse from(JsonValue body) {
    var response = ModelJson.object(body, "response");
    return new KeysClaimResponse(
        claimedKeys(response.get("one_time_keys")), claimFailures(response.get("failures")));
  }

  private static Map<UserId, Map<String, Map<String, ClaimedKey>>> claimedKeys(JsonValue value) {
    JsonObject users = ModelJson.object(value, "one_time_keys");
    Map<UserId, Map<String, Map<String, ClaimedKey>>> result = new LinkedHashMap<>();
    users
        .entrySet()
        .forEach(
            userEntry -> {
              Map<String, Map<String, ClaimedKey>> devices = new LinkedHashMap<>();
              userEntry
                  .getValue()
                  .asObject()
                  .entrySet()
                  .forEach(
                      deviceEntry -> {
                        Map<String, ClaimedKey> keys = new LinkedHashMap<>();
                        deviceEntry
                            .getValue()
                            .asObject()
                            .entrySet()
                            .forEach(
                                keyEntry ->
                                    keys.put(
                                        keyEntry.getKey(), parseClaimedKey(keyEntry.getValue())));
                        devices.put(deviceEntry.getKey(), keys);
                      });
              result.put(UserId.of(userEntry.getKey()), devices);
            });
    return result;
  }

  private static ClaimedKey parseClaimedKey(JsonValue value) {
    if (value.isString()) {
      return new PlainKey(value.asString());
    }
    JsonObject signed = value.asObject();
    var key = ModelJson.requiredString(signed, "key", "Claimed key");
    JsonObject signatures = ModelJson.requiredObject(signed, "signatures", "Claimed key");
    return new SignedKey(key, ModelJson.stringSignatures(signatures));
  }

  private static Map<String, KeysClaimResponse.Failure> claimFailures(JsonValue value) {
    if (value == null) {
      return Map.of();
    }
    JsonObject failures = value.asObject();
    Map<String, Failure> result = new LinkedHashMap<>();
    failures
        .entrySet()
        .forEach(
            entry -> {
              JsonObject failure = entry.getValue().asObject();
              result.put(
                  entry.getKey(),
                  new Failure(
                      ModelJson.requiredString(failure, "errcode", "failure"),
                      ModelJson.requiredString(failure, "error", "failure")));
            });
    return result;
  }
}
