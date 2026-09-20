package io.github.fherbreteau.matrix.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A JSON object backed by a {@link LinkedHashMap} so serialization is deterministic: members keep
 * insertion order (or parse order). All fields of an incoming payload, including unknown ones, are
 * retained and remain accessible via {@link #get(String)} and {@link #names()}.
 */
public final class JsonObject implements JsonValue {

  private final Map<String, JsonValue> values;

  /** Creates an empty JSON object. */
  public JsonObject() {
    this(new LinkedHashMap<>());
  }

  /**
   * Creates a JSON object backed by the given values, preserving their order.
   *
   * @param values the initial members, in order
   */
  public JsonObject(Map<String, JsonValue> values) {
    this.values = values;
  }

  @Override
  public boolean isObject() {
    return true;
  }

  @Override
  public JsonObject asObject() {
    return this;
  }

  /**
   * Returns the value mapped to {@code name}, or {@code null} when absent.
   *
   * @param name the member name
   * @return the value mapped to the name, or {@code null} when absent
   */
  public JsonValue get(String name) {
    return values.get(name);
  }

  /**
   * Returns the value mapped to {@code name}, or {@code fallback} when absent.
   *
   * @param name the member name
   * @param fallback the value returned when the name is absent
   * @return the value mapped to the name, or the fallback
   */
  public JsonValue getOrDefault(String name, JsonValue fallback) {
    return values.getOrDefault(name, fallback);
  }

  /**
   * Associates a value with a name and returns this object for chaining.
   *
   * @param name the member name
   * @param value the value to associate
   * @return this object for chaining
   */
  public JsonObject put(String name, JsonValue value) {
    values.put(name, value);
    return this;
  }

  /**
   * Associates a string value with a name and returns this object for chaining.
   *
   * @param name the member name
   * @param value the string value to associate
   * @return this object for chaining
   */
  public JsonObject put(String name, String value) {
    values.put(name, JsonString.of(value));
    return this;
  }

  /**
   * Associates a number value with a name and returns this object for chaining.
   *
   * @param name the member name
   * @param value the number value to associate
   * @return this object for chaining
   */
  public JsonObject put(String name, long value) {
    values.put(name, JsonNumber.of(value));
    return this;
  }

  /**
   * Associates a number value with a name and returns this object for chaining.
   *
   * @param name the member name
   * @param value the number value to associate
   * @return this object for chaining
   */
  public JsonObject put(String name, double value) {
    values.put(name, JsonNumber.of(value));
    return this;
  }

  /**
   * Associates a boolean value with a name and returns this object for chaining.
   *
   * @param name the member name
   * @param value the boolean value to associate
   * @return this object for chaining
   */
  public JsonObject put(String name, boolean value) {
    values.put(name, JsonBoolean.of(value));
    return this;
  }

  /**
   * Returns whether the object contains a mapping for {@code name}.
   *
   * @param name the member name
   * @return whether the object contains a mapping for the name
   */
  public boolean has(String name) {
    return values.containsKey(name);
  }

  /**
   * Returns the member names, in insertion or parse order.
   *
   * @return the member names, in insertion or parse order
   */
  public Set<String> names() {
    return values.keySet();
  }

  /**
   * Returns the number of members.
   *
   * @return the number of members
   */
  public int size() {
    return values.size();
  }

  /**
   * Returns the members as map entries, in insertion or parse order.
   *
   * @return the members as map entries, in insertion or parse order
   */
  public Set<Map.Entry<String, JsonValue>> entrySet() {
    return values.entrySet();
  }

  @Override
  public String toJson() {
    var sb = new StringBuilder("{");
    var first = true;
    for (var entry : values.entrySet()) {
      if (!first) {
        sb.append(',');
      }
      first = false;
      sb.append(JsonString.of(entry.getKey()).toJson()).append(':');
      sb.append(entry.getValue().toJson());
    }
    return sb.append('}').toString();
  }
}
