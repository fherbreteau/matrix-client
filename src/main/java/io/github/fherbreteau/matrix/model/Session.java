package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A Matrix session as returned by a successful login: the access token with the user ID, device ID
 * and homeserver metadata, plus the raw login response so unknown fields are preserved. The access
 * token never appears in {@link #toString()}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/login">Matrix specification</a>
 */
public record Session(
    String userId,
    String accessToken,
    String refreshToken,
    Long expiresInMs,
    String deviceId,
    String homeserver,
    JsonValue raw) {

  private static final String REFRESH_TOKEN_FIELD = "refresh_token";

  /**
   * Returns whether the access token can be renewed with {@code POST /_matrix/client/v3/refresh},
   * i.e. the server issued a refresh token.
   */
  public boolean isRefreshable() {
    return refreshToken != null;
  }

  /**
   * Parses a login response body into a session.
   *
   * @throws DiscoveryException if the response is missing the access token or user ID
   */
  public static Session from(JsonValue body) {
    if (body == null || !body.isObject()) {
      throw new DiscoveryException("Login response must be a JSON object");
    }
    JsonObject obj = body.asObject();
    JsonValue token = obj.get("access_token");
    JsonValue userId = obj.get("user_id");
    if (token == null || !token.isString() || userId == null || !userId.isString()) {
      throw new DiscoveryException("Login response must contain access_token and user_id");
    }
    return new Session(
        userId.asString(),
        token.asString(),
        stringValue(obj, REFRESH_TOKEN_FIELD),
        longValue(obj, "expires_in_ms"),
        stringValue(obj, "device_id"),
        stringValue(obj, "home_server"),
        body);
  }

  /**
   * Parses a token-refresh response into a session. The refresh response only carries the new
   * tokens, so the user identity and device of the refreshed session are carried over from the
   * session being refreshed; when the response omits a refresh token, the previous one stays valid
   * and is carried over per the specification.
   *
   * @throws DiscoveryException if the response is missing the access token
   */
  public static Session fromRefresh(Session previous, JsonValue body) {
    if (body == null || !body.isObject()) {
      throw new DiscoveryException("Refresh response must be a JSON object");
    }
    JsonObject obj = body.asObject();
    JsonValue token = obj.get("access_token");
    if (token == null || !token.isString()) {
      throw new DiscoveryException("Refresh response must contain access_token");
    }
    return new Session(
        previous.userId(),
        token.asString(),
        stringValue(obj, REFRESH_TOKEN_FIELD) != null
            ? stringValue(obj, REFRESH_TOKEN_FIELD)
            : previous.refreshToken(),
        longValue(obj, "expires_in_ms"),
        previous.deviceId(),
        previous.homeserver(),
        body);
  }

  private static String stringValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isString() ? value.asString() : null;
  }

  private static Long longValue(JsonObject obj, String name) {
    JsonValue value = obj.get(name);
    return value != null && value.isNumber() ? value.asLong() : null;
  }

  /** Returns the identifier of the authenticated user. */
  @Override
  public String userId() {
    return userId;
  }

  /**
   * Returns a representation that never includes the access token or the raw response, both of
   * which carry secrets.
   */
  @Override
  public String toString() {
    return "Session[userId="
        + userId
        + ", accessToken=***"
        + ", refreshToken=***"
        + (expiresInMs != null ? ", expiresInMs=" + expiresInMs : "")
        + (deviceId != null ? ", deviceId=" + deviceId : "")
        + (homeserver != null ? ", homeserver=" + homeserver : "")
        + "]";
  }
}
