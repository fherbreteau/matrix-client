package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Search results and grouping metadata for room events.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3search">Matrix
 *     specification</a>
 */
public record RoomEventsSearchResults(
    long count,
    Map<String, Map<String, SearchGroupValue>> groups,
    List<String> highlights,
    String nextBatch,
    List<SearchResult> results,
    JsonValue state,
    JsonValue raw) {
  /** Copies nested grouping data and result collections to immutable containers. */
  public RoomEventsSearchResults {
    Map<String, Map<String, SearchGroupValue>> immutableGroups = new LinkedHashMap<>();
    groups.forEach(
        (key, value) ->
            immutableGroups.put(key, Collections.unmodifiableMap(new LinkedHashMap<>(value))));
    groups = Collections.unmodifiableMap(immutableGroups);
    highlights = List.copyOf(highlights);
    results = List.copyOf(results);
  }

  /**
   * Parses room-event search results.
   *
   * @param value results JSON
   * @return parsed search results
   * @throws IllegalArgumentException if required fields are missing or malformed
   */
  public static RoomEventsSearchResults from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "room events search results");
    Long count = ModelJson.number(object, "count");
    JsonValue resultValue = object.get("results");
    if (count == null || resultValue == null || !resultValue.isArray()) {
      throw new IllegalArgumentException("room events search results are missing required fields");
    }
    Map<String, Map<String, SearchGroupValue>> groups = parseGroups(object.get("groups"));
    List<SearchResult> results = new ArrayList<>();
    JsonArray resultArray = resultValue.asArray();
    for (int index = 0; index < resultArray.size(); index++) {
      results.add(SearchResult.from(resultArray.get(index)));
    }
    return new RoomEventsSearchResults(
        count,
        groups,
        ModelJson.strings(object.get("highlights"), "search highlights"),
        ModelJson.string(object, "next_batch"),
        results,
        object.get("state"),
        value);
  }

  private static Map<String, Map<String, SearchGroupValue>> parseGroups(JsonValue value) {
    if (value == null) {
      return Map.of();
    }
    if (!value.isObject()) {
      throw new IllegalArgumentException("search groups must be an object");
    }
    Map<String, Map<String, SearchGroupValue>> groups = new LinkedHashMap<>();
    for (String key : value.asObject().names()) {
      JsonValue groupValues = value.asObject().get(key);
      if (!groupValues.isObject()) {
        throw new IllegalArgumentException("search group must be an object");
      }
      Map<String, SearchGroupValue> parsed = new LinkedHashMap<>();
      for (String id : groupValues.asObject().names()) {
        parsed.put(id, SearchGroupValue.from(groupValues.asObject().get(id)));
      }
      groups.put(key, parsed);
    }
    return groups;
  }
}
