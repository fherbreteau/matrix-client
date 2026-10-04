package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;

/**
 * Pagination and recursion parameters for relation queries.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#relationships-api">Matrix
 *     specification</a>
 */
public record RelationsOptions(
    String from, String to, Integer limit, Direction direction, Boolean recurse) {

  /** Returns the default options: backwards direction with server-default limit and recursion. */
  public static RelationsOptions defaults() {
    return new RelationsOptions(null, null, null, Direction.BACKWARD, null);
  }

  /** Serializes set options as query parameters. */
  public JsonObject toQuery() {
    JsonObject query = new JsonObject();
    if (from != null) {
      query.put("from", from);
    }
    if (to != null) {
      query.put("to", to);
    }
    if (limit != null) {
      query.put("limit", limit);
    }
    if (direction != null) {
      query.put("dir", direction.value());
    }
    if (recurse != null) {
      query.put("recurse", recurse);
    }
    return query;
  }
}
