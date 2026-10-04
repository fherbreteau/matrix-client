package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * A group of matching results with opaque pagination state.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record SearchGroupValue(
    String nextBatch, Integer order, List<String> results, JsonValue raw) {
  /** Copies grouped result IDs to an immutable list. */
  public SearchGroupValue {
    results = List.copyOf(results);
  }

  /**
   * Parses a grouping value.
   *
   * @param value grouping JSON
   * @return parsed group value
   * @throws IllegalArgumentException if the value is not an object or has malformed fields
   */
  public static SearchGroupValue from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "search group value");
    Long order = ModelJson.number(object, "order");
    if (order != null && (order < Integer.MIN_VALUE || order > Integer.MAX_VALUE)) {
      throw new IllegalArgumentException("search group order is outside the integer range");
    }
    return new SearchGroupValue(
        ModelJson.string(object, "next_batch"),
        order == null ? null : order.intValue(),
        ModelJson.strings(object.get("results"), "group results"),
        value);
  }
}
