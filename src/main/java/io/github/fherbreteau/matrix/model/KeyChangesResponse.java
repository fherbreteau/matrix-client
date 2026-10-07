package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Users whose device identity keys changed between sync tokens. Missing {@code changed} or {@code
 * left} members are represented as empty lists.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3keyschanges">Matrix
 *     specification</a>
 */
public record KeyChangesResponse(List<UserId> changed, List<UserId> left) {
  /**
   * Creates an immutable changed/left response.
   *
   * @param changed users whose device keys changed
   * @param left users who left
   */
  public KeyChangesResponse {
    changed = List.copyOf(changed);
    left = List.copyOf(left);
  }

  /**
   * Validate and parse a (/key/changes) response body.
   *
   * @param body the parsed response body
   * @return the validated key changes response
   * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
   *     versions} array of strings
   */
  public static KeyChangesResponse from(JsonValue body) {
    var response = ModelJson.object(body, "response");
    return new KeyChangesResponse(userIds(response.get("changed")), userIds(response.get("left")));
  }

  private static List<UserId> userIds(JsonValue value) {
    if (value == null) {
      return List.of();
    }
    if (!value.isArray()) {
      throw new IllegalArgumentException("user ID field must be an array");
    }
    List<UserId> result = new ArrayList<>();
    for (int index = 0; index < value.asArray().size(); index++) {
      result.add(UserId.of(value.asArray().get(index).asString()));
    }
    return List.copyOf(result);
  }
}
