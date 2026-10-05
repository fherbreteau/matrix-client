package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonNumber;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A room tag and its extensible metadata.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#room-tagging">Matrix
 *     specification</a>
 */
public record RoomTag(Map<String, JsonValue> properties) {

  /** Copies tag properties into an immutable insertion-ordered map. */
  public RoomTag {
    properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    validateOrder(properties.get("order"));
  }

  private static void validateOrder(JsonValue order) {
    if (order != null
        && (!order.isNumber()
            || order.asBigDecimal().signum() < 0
            || order.asBigDecimal().compareTo(BigDecimal.ONE) > 0)) {
      throw new IllegalArgumentException("tag order must be a number between 0 and 1");
    }
  }

  /**
   * Creates a tag with no metadata.
   *
   * @return an empty tag
   */
  public static RoomTag empty() {
    return new RoomTag(Map.of());
  }

  /**
   * Creates a tag with its ordering value.
   *
   * @param order the display order, between 0 and 1
   * @return the tag
   */
  public static RoomTag withOrder(double order) {
    JsonNumber orderValue = JsonNumber.of(order);
    validateOrder(orderValue);
    return new RoomTag(Map.of("order", orderValue));
  }

  /**
   * Parses a room-tag object.
   *
   * @param value the response or request JSON
   * @return the parsed tag
   * @throws IllegalArgumentException if the value is not an object or its order is invalid
   */
  public static RoomTag from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("room tag must be a JSON object");
    }
    Map<String, JsonValue> properties = new LinkedHashMap<>();
    value.asObject().entrySet().forEach(entry -> properties.put(entry.getKey(), entry.getValue()));
    return new RoomTag(properties);
  }

  /**
   * Returns the optional tag ordering value.
   *
   * @return the ordering value, or {@code null} if not set
   */
  public Double order() {
    JsonValue value = properties.get("order");
    return value == null ? null : value.asDouble();
  }

  /**
   * Serializes the tag properties.
   *
   * @return the tag as JSON
   */
  public JsonObject toJson() {
    return new JsonObject(new LinkedHashMap<>(properties));
  }
}
