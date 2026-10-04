package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A user returned by user-directory search. Unknown fields remain in {@link #raw()}.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3user_directorysearch">Matrix
 *     specification</a>
 */
public record DirectoryUser(UserId userId, String displayName, String avatarUrl, JsonValue raw) {
  /**
   * Parses a directory user.
   *
   * @param value user JSON
   * @return parsed user
   * @throws IllegalArgumentException if the required user ID is malformed or missing
   */
  public static DirectoryUser from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "directory user");
    return new DirectoryUser(
        UserId.of(ModelJson.requiredString(object, "user_id", "directory user")),
        ModelJson.string(object, "display_name"),
        ModelJson.string(object, "avatar_url"),
        value);
  }
}
