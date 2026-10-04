package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonString;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A user-directory search request.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3user_directorysearch">Matrix
 *     specification</a>
 */
public record UserDirectorySearchRequest(String searchTerm, Integer limit) {
  /** Validates required search text and a positive optional result limit. */
  public UserDirectorySearchRequest {
    if (searchTerm == null) {
      throw new IllegalArgumentException("searchTerm is required");
    }
    if (limit != null && limit <= 0) {
      throw new IllegalArgumentException("limit must be greater than zero");
    }
  }

  /**
   * Serializes the request body.
   *
   * @return request JSON
   */
  public JsonValue toJson() {
    JsonObject body = new JsonObject().put("search_term", JsonString.of(searchTerm));
    if (limit != null) {
      body.put("limit", limit);
    }
    return body;
  }
}
