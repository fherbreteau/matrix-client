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
 * A cross-signing key object.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysquery">Matrix
 *     specification</a>
 */
public record CrossSigningKey(
    List<String> usage,
    UserId userId,
    Map<String, String> keys,
    Map<UserId, Map<String, String>> signatures)
    implements SignedObject {
  private static final String DESCRIPTION = "cross-signing key";
  private static final String KEYS_KEY = "keys";
  private static final String SIGNATURES_KEY = "signatures";
  private static final String USAGE_KEY = "usage";
  private static final String USER_ID_KEY = "user_id";

  /**
   * Creates a cross-signing key.
   *
   * @param usage cross-signing key usages
   * @param userId owning user identifier
   * @param keys public keys
   * @param signatures signatures by user and key ID
   */
  public CrossSigningKey {
    usage = ModelJson.requireNotEmpty(immutableList(usage));
    keys = ModelJson.requireNotEmpty(immutableMap(keys));
    signatures = immutableMap(signatures);
    userId = requireNonNull(userId);
  }

  @Override
  public JsonValue toJson() {
    JsonObject request = new JsonObject();
    // Serialize the Keys
    request.put(KEYS_KEY, ModelJson.toObject(keys));
    // Serialize the signatures
    request.put(SIGNATURES_KEY, ModelJson.toObject(signatures));
    // Serialize the usage
    request.put(USAGE_KEY, ModelJson.toArray(usage));
    // Serialize the user
    request.put(USER_ID_KEY, userId.value());
    return request;
  }

  @Override
  public JsonValue toUnsignedJson() {
    JsonObject request = new JsonObject();
    request.put(KEYS_KEY, ModelJson.toObject(keys));
    request.put(USAGE_KEY, ModelJson.toArray(usage));
    request.put(USER_ID_KEY, userId.value());
    return request;
  }

  @Override
  public CrossSigningKey withSignatures(Map<UserId, Map<String, String>> signatures) {
    return new CrossSigningKey(this.usage, this.userId, this.keys, signatures);
  }

  /**
   * Validate and parse a cross signing key.
   *
   * @param value the parsed cross-signing key object
   * @return the validated cross signing key
   * @throws DiscoveryException if the body is not a JSON object or does not contain a cross signing
   *     key
   */
  public static CrossSigningKey from(JsonValue value) {
    var key = ModelJson.object(value, DESCRIPTION);
    JsonValue usageValue = key.get(USAGE_KEY);
    if (usageValue == null || !usageValue.isArray()) {
      throw new IllegalArgumentException(DESCRIPTION + " must contain " + USAGE_KEY);
    }
    var usage = ModelJson.strings(usageValue, USAGE_KEY);
    var userId = ModelJson.requiredString(key, USER_ID_KEY, DESCRIPTION);
    var keys = ModelJson.requiredStringMap(key.get(KEYS_KEY), DESCRIPTION);
    var signatures = ModelJson.stringSignatures(key.get(SIGNATURES_KEY));
    return new CrossSigningKey(usage, UserId.of(userId), keys, signatures);
  }
}
