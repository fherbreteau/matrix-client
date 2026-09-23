package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A user profile with display name and avatar URL. Unknown fields of the response are preserved in
 * the raw value.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/get-matrixclientv3profileuserid">Matrix
 *     specification</a>
 */
public record UserProfile(String displayName, String avatarUrl, JsonValue raw) {

  /**
   * Parses a profile response.
   *
   * @throws IllegalArgumentException if the value is not a JSON object
   */
  public static UserProfile from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("profile must be a JSON object");
    }
    JsonObject obj = value.asObject();
    JsonValue displayName = obj.get("displayname");
    JsonValue avatarUrl = obj.get("avatar_url");
    return new UserProfile(
        displayName != null && displayName.isString() ? displayName.asString() : null,
        avatarUrl != null && avatarUrl.isString() ? avatarUrl.asString() : null,
        value);
  }
}
