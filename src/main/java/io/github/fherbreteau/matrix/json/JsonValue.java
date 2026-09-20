package io.github.fherbreteau.matrix.json;

import java.math.BigDecimal;

/**
 * A minimal JSON value. Implemented by the object, array, string, number, boolean and null
 * representations produced by {@link JsonParser}.
 */
public sealed interface JsonValue
    permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {

  default boolean isObject() {
    return false;
  }

  default boolean isArray() {
    return false;
  }

  default boolean isString() {
    return false;
  }

  default boolean isNumber() {
    return false;
  }

  default boolean isBoolean() {
    return false;
  }

  default boolean isNull() {
    return false;
  }

  /**
   * Returns this value as a {@link JsonObject}.
   *
   * @return this value as a {@link JsonObject}
   */
  default JsonObject asObject() {
    throw new UnsupportedOperationException("Not a JSON object");
  }

  /**
   * Returns this value as a {@link JsonArray}.
   *
   * @return this value as a {@link JsonArray}
   */
  default JsonArray asArray() {
    throw new UnsupportedOperationException("Not a JSON array");
  }

  /**
   * Returns this value as a string.
   *
   * @return this value as a string
   */
  default String asString() {
    throw new UnsupportedOperationException("Not a JSON string");
  }

  /**
   * Returns this value as a double.
   *
   * @return this value as a double
   */
  default double asDouble() {
    throw new UnsupportedOperationException("Not a JSON number");
  }

  /**
   * Returns this value as a long.
   *
   * @return this value as a long
   */
  default long asLong() {
    throw new UnsupportedOperationException("Not a JSON number");
  }

  /**
   * Returns this value as a {@link BigDecimal} preserving full precision.
   *
   * @return this value as a {@link BigDecimal} preserving full precision
   */
  default BigDecimal asBigDecimal() {
    throw new UnsupportedOperationException("Not a JSON number");
  }

  /**
   * Returns this value as a boolean.
   *
   * @return this value as a boolean
   */
  default boolean asBoolean() {
    throw new UnsupportedOperationException("Not a JSON boolean");
  }

  /**
   * Serializes this value back to a JSON string.
   *
   * @return the JSON serialization of this value
   */
  String toJson();
}
