package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Result of Matrix account registration. Login credentials are absent when registration inhibits
 * login; unknown response fields remain available in {@link #raw()}.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3register">Matrix
 *     specification</a>
 */
public record RegistrationResponse(
    String userId,
    String accessToken,
    String deviceId,
    String refreshToken,
    Long expiresInMs,
    JsonValue raw) {

  /** Parses a successful registration response. */
  public static RegistrationResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new DiscoveryException("Registration response must be a JSON object");
    }
    JsonObject object = value.asObject();
    JsonValue userId = object.get("user_id");
    if (userId == null || !userId.isString()) {
      throw new DiscoveryException("Registration response must contain user_id");
    }
    return new RegistrationResponse(
        userId.asString(),
        stringValue(object, "access_token"),
        stringValue(object, "device_id"),
        stringValue(object, "refresh_token"),
        longValue(object, "expires_in_ms"),
        value);
  }

  private static String stringValue(JsonObject object, String field) {
    JsonValue value = object.get(field);
    return value != null && value.isString() ? value.asString() : null;
  }

  private static Long longValue(JsonObject object, String field) {
    JsonValue value = object.get(field);
    return value != null && value.isNumber() ? value.asLong() : null;
  }
}
