package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The members currently joined to a room, as returned by {@code /rooms/{roomId}/joined_members}.
 * Unknown fields of the response are ignored; display names may be {@code null} when absent.
 */
public record JoinedMembers(Map<String, String> displayNames) {

  /**
   * Parses a {@code joined_members} response.
   *
   * @throws IllegalArgumentException if the value is not a JSON object
   */
  public static JoinedMembers from(JsonValue value) {
    JsonObject obj = value.asObject();
    var displayNames = new LinkedHashMap<String, String>();
    JsonValue joined = obj.get("joined");
    if (joined != null && joined.isObject()) {
      for (Map.Entry<String, JsonValue> entry : joined.asObject().entrySet()) {
        JsonValue member = entry.getValue();
        String displayName = null;
        if (member.isObject()) {
          JsonValue name = member.asObject().get("display_name");
          if (name != null && name.isString()) {
            displayName = name.asString();
          }
        }
        displayNames.put(entry.getKey(), displayName);
      }
    }
    return new JoinedMembers(displayNames);
  }

  /** Returns the display name of a joined member, or {@code null} when absent. */
  public String displayNameOf(String userId) {
    return displayNames.get(userId);
  }
}
