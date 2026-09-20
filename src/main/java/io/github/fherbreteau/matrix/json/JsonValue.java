package io.github.fherbreteau.matrix.json;

import java.math.BigDecimal;

/**
 * A minimal JSON value. Implemented by the object, array, string, number, boolean and null
 * representations produced by {@link JsonParser}.
 */
public sealed interface JsonValue
    permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {

  /**
   * Returns whether this value is a JSON object.
   *
   * @return {@code true} if this value is a {@link JsonObject}
   */
  default boolean isObject() {
    return false;
  }

  /**
   * Returns whether this value is a JSON array.
   *
   * @return {@code true} if this value is a {@link JsonArray}
   */
  default boolean isArray() {
    return false;
  }

  /**
   * Returns whether this value is a JSON string.
   *
   * @return {@code true} if this value is a {@link JsonString}
   */
  default boolean isString() {
    return false;
  }

  /**
   * Returns whether this value is a JSON number.
   *
   * @return {@code true} if this value is a {@link JsonNumber}
   */
  default boolean isNumber() {
    return false;
  }

  /**
   * Returns whether this value is a JSON boolean.
   *
   * @return {@code true} if this value is a {@link JsonBoolean}
   */
  default boolean isBoolean() {
    return false;
  }

  /**
   * Returns whether this value is the JSON {@code null} value.
   *
   * @return {@code true} if this value is {@link JsonNull}
   */
  default boolean isNull() {
    return false;
  }

  /**
   * Returns this value as a JSON object.
   *
   * @return this value as a {@link JsonObject}
   * @throws UnsupportedOperationException if this value is not a JSON object
   */
  default JsonObject asObject() {
    throw new UnsupportedOperationException("Not a JSON object");
  }

  /**
   * Returns this value as a JSON array.
   *
   * @return this value as a {@link JsonArray}
   * @throws UnsupportedOperationException if this value is not a JSON array
   */
  default JsonArray asArray() {
    throw new UnsupportedOperationException("Not a JSON array");
  }

  /**
   * Returns this value as a string.
   *
   * @return the string value
   * @throws UnsupportedOperationException if this value is not a JSON string
   */
  default String asString() {
    throw new UnsupportedOperationException("Not a JSON string");
  }

  /**
   * Returns this value as a double.
   *
   * @return the numeric value as a double
   * @throws UnsupportedOperationException if this value is not a JSON number
   */
  default double asDouble() {
    throw new UnsupportedOperationException("Not a JSON number");
  }

  /**
   * Returns this value as a long, truncating any fractional part.
   *
   * @return the numeric value as a long
   * @throws UnsupportedOperationException if this value is not a JSON number
   */
  default long asLong() {
    throw new UnsupportedOperationException("Not a JSON number");
  }

  /**
   * Returns this value as an arbitrary-precision decimal, preserving full numeric precision.
   *
   * @return the numeric value as a {@link BigDecimal}
   * @throws UnsupportedOperationException if this value is not a JSON number
   */
  default BigDecimal asBigDecimal() {
    throw new UnsupportedOperationException("Not a JSON number");
  }

  /**
   * Returns this value as a boolean.
   *
   * @return the boolean value
   * @throws UnsupportedOperationException if this value is not a JSON boolean
   */
  default boolean asBoolean() {
    throw new UnsupportedOperationException("Not a JSON boolean");
  }

  /**
   * Serializes this value back to a JSON string with valid escaping.
   *
   * @return the JSON serialization of this value
   */
  String toJson();
}
