package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;

/**
 * A set of pagination and depth options for a space hierarchy request.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidhierarchy">Matrix
 *     specification</a>
 */
public record SpaceHierarchyOptions(
    String from, Integer limit, Integer maxDepth, Boolean suggestedOnly) {
  /** Validates hierarchy limits against the specification. */
  public SpaceHierarchyOptions {
    if (limit != null && limit <= 0) {
      throw new IllegalArgumentException("limit must be greater than zero");
    }
    if (maxDepth != null && maxDepth < 0) {
      throw new IllegalArgumentException("maxDepth must not be negative");
    }
  }

  /**
   * Serializes present options using the endpoint query parameter names.
   *
   * @return query parameters with opaque pagination token preserved
   */
  public JsonObject toQuery() {
    JsonObject query = new JsonObject();
    if (from != null) {
      query.put("from", from);
    }
    if (limit != null) {
      query.put("limit", limit);
    }
    if (maxDepth != null) {
      query.put("max_depth", maxDepth);
    }
    if (suggestedOnly != null) {
      query.put("suggested_only", suggestedOnly);
    }
    return query;
  }
}
