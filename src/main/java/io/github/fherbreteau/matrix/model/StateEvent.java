package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A state event configuring a room, such as the room name, topic or join rules. State events carry
 * a type, a state key (empty for most room-level settings) and a content object. Use {@link
 * #of(String, JsonValue)} for events with an empty state key and {@link #of(String, String,
 * JsonValue)} for events with an explicit state key.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3roomsroomidstateeventtypestatekey">Matrix
 *     specification</a>
 */
public record StateEvent(String type, String stateKey, JsonValue content) {

  /** Creates a validated state event with an empty state key. */
  public StateEvent {
    requireNonBlank(type, "type");
    if (stateKey == null) {
      stateKey = "";
    }
    if (content == null) {
      content = new JsonObject();
    }
  }

  /**
   * Creates a state event with an empty state key.
   *
   * @param type the event type, such as {@code m.room.name}
   * @param content the event content
   * @return the state event
   * @throws IllegalArgumentException if the type is blank
   */
  public static StateEvent of(String type, JsonValue content) {
    return new StateEvent(type, "", content);
  }

  /**
   * Creates a state event with an explicit state key, for state events scoped to a user or another
   * sub-key.
   *
   * @param type the event type
   * @param stateKey the state key
   * @param content the event content
   * @return the state event
   * @throws IllegalArgumentException if the type is blank
   */
  public static StateEvent of(String type, String stateKey, JsonValue content) {
    return new StateEvent(type, stateKey, content);
  }

  /**
   * Serializes the state event as an {@code initial_state} entry.
   *
   * @return the state event as a JSON object
   */
  public JsonValue toJson() {
    return new JsonObject().put("type", type).put("state_key", stateKey).put("content", content);
  }

  private static void requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
  }
}
