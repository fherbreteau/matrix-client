package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Event context options for a server-side search request.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record SearchEventContext(Integer beforeLimit, Integer afterLimit, Boolean includeProfile) {
  /** Validates non-negative event-context limits. */
  public SearchEventContext {
    if (beforeLimit != null && beforeLimit < 0) {
      throw new IllegalArgumentException("beforeLimit must not be negative");
    }
    if (afterLimit != null && afterLimit < 0) {
      throw new IllegalArgumentException("afterLimit must not be negative");
    }
  }

  /**
   * Serializes configured event-context options.
   *
   * @return JSON options
   */
  public JsonValue toJson() {
    JsonObject object = new JsonObject();
    if (beforeLimit != null) {
      object.put("before_limit", beforeLimit);
    }
    if (afterLimit != null) {
      object.put("after_limit", afterLimit);
    }
    if (includeProfile != null) {
      object.put("include_profile", includeProfile);
    }
    return object;
  }

  /**
   * Returns a context with specification defaults.
   *
   * @return context including five events before and after results, without profile data
   */
  public static SearchEventContext defaults() {
    return new SearchEventContext(5, 5, false);
  }
}
