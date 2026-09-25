package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;

/**
 * Parameters of a `/sync` call. All fields are optional; the homeserver defaults `timeout` to zero,
 * `full_state` to false, and `set_presence` to online behavior when omitted.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#syncing">Matrix specification</a>
 */
public record SyncOptions(
    String since,
    String filter,
    Long timeoutMs,
    Boolean fullState,
    String setPresence,
    Boolean useStateAfter) {

  /** Creates options that allow the homeserver to use its defaults. */
  public static SyncOptions defaults() {
    return new SyncOptions(null, null, null, null, null, null);
  }

  /**
   * Creates an initial sync with no token, waiting up to the supplied long-poll timeout.
   *
   * @param timeoutMs timeout in milliseconds; non-positive values mean no long polling
   * @param filter saved filter ID or serialized inline filter, or null for no filter
   * @return initial-sync options
   */
  public static SyncOptions initial(long timeoutMs, String filter) {
    return new SyncOptions(null, filter, timeoutMs, false, null, null);
  }

  /**
   * Creates an incremental sync from a previous opaque `next_batch` token.
   *
   * @param since the previous opaque sync token
   * @param timeoutMs timeout in milliseconds; non-positive values mean no long polling
   * @param filter saved filter ID or serialized inline filter, or null for no filter
   * @return incremental-sync options
   */
  public static SyncOptions incremental(String since, long timeoutMs, String filter) {
    if (since == null || since.isBlank()) {
      throw new IllegalArgumentException("since token is required for incremental sync");
    }
    return new SyncOptions(since, filter, timeoutMs, false, null, null);
  }

  /** Serializes only specified options into query parameters. */
  public JsonObject toQuery() {
    var query = new JsonObject();
    if (since != null) {
      query.put("since", since);
    }
    if (filter != null) {
      query.put("filter", filter);
    }
    if (timeoutMs != null && timeoutMs > 0) {
      query.put("timeout", timeoutMs);
    }
    if (fullState != null) {
      query.put("full_state", fullState);
    }
    if (setPresence != null) {
      query.put("set_presence", setPresence);
    }
    if (useStateAfter != null) {
      query.put("use_state_after", useStateAfter);
    }
    return query;
  }
}
