package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonString;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * A grouping requested for server-side search.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record SearchGrouping(List<String> groupBy) {
  /** Copies grouping keys to an immutable list. */
  public SearchGrouping {
    groupBy = List.copyOf(groupBy);
  }

  /**
   * Serializes grouping options.
   *
   * @return grouping JSON
   */
  public JsonValue toJson() {
    JsonArray groups = new JsonArray();
    groupBy.forEach(key -> groups.add(new JsonObject().put("key", JsonString.of(key))));
    return new JsonObject().put("group_by", groups);
  }
}
