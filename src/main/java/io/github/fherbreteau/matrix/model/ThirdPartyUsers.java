package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Third-party users matching a lookup query.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyuser">Matrix
 *     specification</a>
 */
public record ThirdPartyUsers(List<ThirdPartyUser> users, JsonValue raw) {

  /** Copies users to an immutable list. */
  public ThirdPartyUsers {
    users = List.copyOf(users);
  }

  /**
   * Parses an array of third-party users.
   *
   * @param value the response JSON
   * @return the parsed users
   * @throws IllegalArgumentException if the response is not an array
   */
  public static ThirdPartyUsers from(JsonValue value) {
    if (value == null || !value.isArray()) {
      throw new IllegalArgumentException("third-party users must be a JSON array");
    }
    JsonArray array = value.asArray();
    List<ThirdPartyUser> users = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      users.add(ThirdPartyUser.from(array.get(index)));
    }
    return new ThirdPartyUsers(users, value);
  }
}
