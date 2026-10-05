package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Users whose device identity keys changed between two sync tokens.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3keyschanges">Matrix
 *     specification</a>
 */
public record KeyChangesResponse(List<UserId> changed, List<UserId> left, JsonValue raw) {

  /** Copies user ID lists into immutable lists. */
  public KeyChangesResponse {
    changed = List.copyOf(changed);
    left = List.copyOf(left);
  }

  /**
   * Parses a key-changes response.
   *
   * @param value the response JSON
   * @return the parsed response
   * @throws IllegalArgumentException if the response or either user list is malformed
   */
  public static KeyChangesResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "key changes response");
    return new KeyChangesResponse(
        optionalUserIds(object, "changed"), optionalUserIds(object, "left"), value);
  }

  private static List<UserId> optionalUserIds(JsonObject object, String field) {
    JsonValue value = object.get(field);
    if (value == null) {
      return List.of();
    }
    if (!value.isArray()) {
      throw new IllegalArgumentException("key changes response " + field + " must be an array");
    }
    JsonArray array = value.asArray();
    List<UserId> ids = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      JsonValue item = array.get(index);
      if (!item.isString()) {
        throw new IllegalArgumentException("key changes " + field + " must contain user IDs");
      }
      ids.add(UserId.of(item.asString()));
    }
    return ids;
  }
}
