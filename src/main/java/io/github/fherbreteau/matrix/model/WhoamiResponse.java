package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * The identity the current access token belongs to, as returned by {@code GET
 * /_matrix/client/v3/account/whoami}. Unknown fields of the response are preserved in the raw
 * value.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/get-matrixclientv3account-whoami">Matrix
 *     specification</a>
 */
public record WhoamiResponse(String userId, String deviceId, boolean guest, JsonValue raw) {

  /**
   * Parses a whoami response.
   *
   * @throws IllegalArgumentException if the value is not a JSON object or does not carry a user_id
   */
  public static WhoamiResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("whoami response must be a JSON object");
    }
    JsonObject obj = value.asObject();
    JsonValue userId = obj.get("user_id");
    if (userId == null || !userId.isString()) {
      throw new IllegalArgumentException("whoami response must contain a user_id");
    }
    JsonValue deviceId = obj.get("device_id");
    JsonValue isGuest = obj.get("is_guest");
    return new WhoamiResponse(
        userId.asString(),
        deviceId != null && deviceId.isString() ? deviceId.asString() : null,
        isGuest != null && isGuest.isBoolean() && isGuest.asBoolean(),
        value);
  }
}
