package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;

/**
 * Pagination and participation filter for listing thread roots in a room.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidthreads">Matrix
 *     specification</a>
 */
public record ThreadsOptions(String from, Integer limit, Include include) {

  /** Which thread roots to include. */
  public enum Include {
    ALL("all"),
    PARTICIPATED("participated");

    private final String value;

    Include(String value) {
      this.value = value;
    }

    /**
     * Returns the wire value.
     *
     * @return the string representation sent to the homeserver
     */
    public String value() {
      return value;
    }
  }

  /**
   * Returns default options with all thread roots included.
   *
   * @return options for the initial page of all threads
   */
  public static ThreadsOptions defaults() {
    return new ThreadsOptions(null, null, Include.ALL);
  }

  /** Serializes specified parameters as a query object. */
  public JsonObject toQuery() {
    JsonObject query = new JsonObject();
    if (from != null) {
      query.put("from", from);
    }
    if (limit != null) {
      query.put("limit", limit);
    }
    if (include != null) {
      query.put("include", include.value());
    }
    return query;
  }
}
