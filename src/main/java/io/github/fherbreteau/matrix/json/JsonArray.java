package io.github.fherbreteau.matrix.json;

import java.util.ArrayList;
import java.util.List;

/** A JSON array. */
public final class JsonArray implements JsonValue {

  private final List<JsonValue> values;

  /** Creates an empty JSON array. */
  public JsonArray() {
    this(new ArrayList<>());
  }

  /**
   * Creates a JSON array backed by the given values.
   *
   * @param values the initial elements of the array
   */
  public JsonArray(List<JsonValue> values) {
    this.values = values;
  }

  @Override
  public boolean isArray() {
    return true;
  }

  @Override
  public JsonArray asArray() {
    return this;
  }

  /**
   * Returns the value at the given index.
   *
   * @param index the element index
   * @return the value at the given index
   */
  public JsonValue get(int index) {
    return values.get(index);
  }

  /**
   * Appends a value and returns this array for chaining.
   *
   * @param value the value to append
   * @return this array for chaining
   */
  public JsonArray add(JsonValue value) {
    values.add(value);
    return this;
  }

  /**
   * Returns the number of elements.
   *
   * @return the number of elements
   */
  public int size() {
    return values.size();
  }

  @Override
  public String toJson() {
    var sb = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(values.get(i).toJson());
    }
    return sb.append(']').toString();
  }
}
