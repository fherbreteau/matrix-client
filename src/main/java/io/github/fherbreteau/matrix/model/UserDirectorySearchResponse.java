package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A user-directory search response.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3user_directorysearch">Matrix
 *     specification</a>
 */
public record UserDirectorySearchResponse(
    boolean limited, List<DirectoryUser> results, JsonValue raw) {
  /** Copies search results to an immutable list. */
  public UserDirectorySearchResponse {
    results = List.copyOf(results);
  }

  /**
   * Parses a user-directory search response.
   *
   * @param value response JSON
   * @return parsed response
   * @throws IllegalArgumentException if required response fields are missing or malformed
   */
  public static UserDirectorySearchResponse from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "user directory response");
    Boolean limited = ModelJson.bool(object, "limited");
    JsonValue resultValue = object.get("results");
    if (limited == null || resultValue == null || !resultValue.isArray()) {
      throw new IllegalArgumentException("user directory response is missing required fields");
    }
    JsonArray array = resultValue.asArray();
    List<DirectoryUser> results = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      results.add(DirectoryUser.from(array.get(index)));
    }
    return new UserDirectorySearchResponse(limited, results, value);
  }
}
