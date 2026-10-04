package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonString;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

final class ModelJson {
  private ModelJson() {}

  static JsonObject object(JsonValue value, String description) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException(description + " must be a JSON object");
    }
    return value.asObject();
  }

  static String requiredString(JsonObject object, String key, String description) {
    JsonValue value = object.get(key);
    if (value == null || !value.isString()) {
      throw new IllegalArgumentException(description + " must contain " + key);
    }
    return value.asString();
  }

  static String string(JsonObject object, String key) {
    JsonValue value = object.get(key);
    return value != null && value.isString() ? value.asString() : null;
  }

  static Long number(JsonObject object, String key) {
    JsonValue value = object.get(key);
    if (value == null) {
      return null;
    }
    if (!value.isNumber()) {
      throw new IllegalArgumentException(key + " must be an integer");
    }
    if (value.asBigDecimal().stripTrailingZeros().scale() > 0) {
      throw new IllegalArgumentException(key + " must be an integer");
    }
    try {
      return value.asBigDecimal().longValueExact();
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          key + " must be an integer within the long range", exception);
    }
  }

  static Boolean bool(JsonObject object, String key) {
    JsonValue value = object.get(key);
    return value != null && value.isBoolean() ? value.asBoolean() : null;
  }

  static List<String> strings(JsonValue value, String description) {
    if (value == null) {
      return List.of();
    }
    if (!value.isArray()) {
      throw new IllegalArgumentException(description + " must be an array");
    }
    JsonArray array = value.asArray();
    List<String> strings = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      JsonValue item = array.get(index);
      if (!item.isString()) {
        throw new IllegalArgumentException(description + " must contain strings");
      }
      strings.add(item.asString());
    }
    return List.copyOf(strings);
  }

  static JsonArray toArray(List<String> values) {
    JsonArray array = new JsonArray();
    values.forEach(value -> array.add(JsonString.of(value)));
    return array;
  }
}
