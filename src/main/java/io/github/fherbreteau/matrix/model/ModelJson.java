package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonBoolean;
import io.github.fherbreteau.matrix.json.JsonNull;
import io.github.fherbreteau.matrix.json.JsonNumber;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonString;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModelJson {
  private ModelJson() {}

  static <T> List<T> requireNotEmpty(List<T> collection) {
    if (collection == null || collection.isEmpty()) {
      throw new IllegalArgumentException("collection should not be empty nor null");
    }
    return collection;
  }

  static <K, V> Map<K, V> requireNotEmpty(Map<K, V> map) {
    if (map == null || map.isEmpty()) {
      throw new IllegalArgumentException("map should not be empty nor null");
    }
    return map;
  }

  static JsonObject object(JsonValue value, String description) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException(description + " must be a JSON object");
    }
    return value.asObject();
  }

  static JsonObject requiredObject(JsonObject object, String key, String description) {
    JsonValue value = object.get(key);
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException(description + " must contain " + key);
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

  static String optionalString(JsonObject object, String key) {
    JsonValue value = object.get(key);
    return value != null && value.isString() ? value.asString() : null;
  }

  static String string(JsonObject object, String key) {
    return optionalString(object, key);
  }

  static Long number(JsonObject object, String key) {
    JsonValue value = object.get(key);
    return value == null ? null : number(value, key);
  }

  static Long number(JsonValue value, String description) {
    if (value == null) {
      throw new IllegalArgumentException(description + " must not be null");
    }
    if (!value.isNumber()) {
      throw new IllegalArgumentException(description + " must be an integer");
    }
    if (value.asBigDecimal().stripTrailingZeros().scale() > 0) {
      throw new IllegalArgumentException(description + " must be an integer");
    }
    try {
      return value.asBigDecimal().longValueExact();
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          description + " must be an integer within the long range", exception);
    }
  }

  static Long requiredNumber(JsonObject object, String key, String description) {
    JsonValue value = object.get(key);
    if (value == null) {
      throw new IllegalArgumentException(description + " must contain " + key);
    }
    return number(value, key);
  }

  static Long optionalNumber(JsonObject object, String key) {
    JsonValue value = object.get(key);
    if (value == null) {
      return null;
    }
    return number(value, key);
  }

  static Boolean bool(JsonObject object, String key) {
    JsonValue value = object.get(key);
    return value != null && value.isBoolean() ? value.asBoolean() : null;
  }

  static Boolean requiredBoolean(JsonObject object, String key) {
    JsonValue value = object.get(key);
    if (value == null || !value.isBoolean()) {
      throw new IllegalArgumentException(key + " must be a boolean");
    }
    return value.asBoolean();
  }

  static Boolean optionalBoolean(JsonObject object, String key) {
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

  static Map<UserId, Map<String, String>> stringSignatures(JsonValue value) {
    if (value == null) {
      return null;
    }
    JsonObject users = object(value, "signatures");
    Map<UserId, Map<String, String>> result = new LinkedHashMap<>();
    users
        .entrySet()
        .forEach(
            entry ->
                result.put(
                    UserId.of(entry.getKey()),
                    optionalStringMap(object(entry.getValue(), "signatures"))));
    return result;
  }

  static Map<String, String> requiredStringMap(JsonValue value, String description) {
    if (value == null) {
      throw new IllegalArgumentException(description + " must contain key/value pairs");
    }
    JsonObject object = value.asObject();
    Map<String, String> result = new LinkedHashMap<>();
    object
        .entrySet()
        .forEach(
            entry -> {
              if (!entry.getValue().isString()) {
                throw new IllegalArgumentException(description + " must contain string values");
              }
              result.put(entry.getKey(), entry.getValue().asString());
            });
    return result;
  }

  static Map<String, String> optionalStringMap(JsonValue value) {
    if (value == null) {
      return Map.of();
    }
    JsonObject object = object(value, "key/value pairs");
    Map<String, String> result = new LinkedHashMap<>();
    object
        .entrySet()
        .forEach(
            entry -> {
              if (!entry.getValue().isString()) {
                throw new IllegalArgumentException("key/value pairs must contain string values");
              }
              result.put(entry.getKey(), entry.getValue().asString());
            });
    return result;
  }

  static Map<String, Object> optionalTypedMapExcept(JsonObject object, String... excluded) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (var entry : object.entrySet()) {
      if (isExcluded(entry.getKey(), excluded)) {
        continue;
      }
      result.put(entry.getKey(), fromJsonValue(entry.getValue()));
    }
    return result.isEmpty() ? null : result;
  }

  private static boolean isExcluded(String key, String[] excluded) {
    for (String field : excluded) {
      if (field.equals(key)) {
        return true;
      }
    }
    return false;
  }

  static Object fromJsonValue(JsonValue value) {
    if (value.isObject()) {
      Map<String, Object> result = new LinkedHashMap<>();
      value
          .asObject()
          .entrySet()
          .forEach(entry -> result.put(entry.getKey(), fromJsonValue(entry.getValue())));
      return result;
    }
    if (value.isArray()) {
      List<Object> result = new ArrayList<>();
      for (int index = 0; index < value.asArray().size(); index++) {
        result.add(fromJsonValue(value.asArray().get(index)));
      }
      return List.copyOf(result);
    }
    if (value.isString()) {
      return value.asString();
    }
    if (value.isNumber()) {
      return value.asBigDecimal();
    }
    if (value.isBoolean()) {
      return value.asBoolean();
    }
    return null;
  }

  static <L> JsonArray toArray(List<L> values) {
    JsonArray array = new JsonArray();
    values.forEach(value -> array.add(toJsonValue(value)));
    return array;
  }

  static <K, V> JsonObject toObject(Map<K, V> values) {
    JsonObject object = new JsonObject();
    values.forEach((key, value) -> object.put(serializeKey(key), toJsonValue(value)));
    return object;
  }

  static JsonValue toJsonValue(Object value) {
    if (value == null) {
      return JsonNull.INSTANCE;
    }
    if (value instanceof String string) {
      return JsonString.of(string);
    }
    if (value instanceof Boolean booleanValue) {
      return JsonBoolean.of(booleanValue);
    }
    if (value instanceof BigDecimal number) {
      return JsonNumber.of(number);
    }
    if (value instanceof Long number) {
      return JsonNumber.of(number);
    }
    if (value instanceof Integer number) {
      return JsonNumber.of(number.longValue());
    }
    if (value instanceof Double number) {
      return JsonNumber.of(number);
    }
    if (value instanceof Number number) {
      return JsonNumber.of(number.doubleValue());
    }
    if (value instanceof Serializable serializable) {
      return serializable.toJson();
    }
    if (value instanceof List<?> values) {
      return toArray(values);
    }
    if (value instanceof Map<?, ?> values) {
      return toObject(values);
    }
    throw new IllegalArgumentException("unsupported typed JSON value");
  }

  @SuppressWarnings("java:S6880")
  private static String serializeKey(Object object) {
    if (object instanceof String string) {
      return string;
    } else if (object instanceof RoomId roomId) {
      return roomId.value();
    } else if (object instanceof UserId userId) {
      return userId.value();
    } else if (object instanceof DeviceId deviceId) {
      return deviceId.value();
    } else if (object instanceof EventId eventId) {
      return eventId.value();
    } else {
      throw new IllegalArgumentException(object.getClass() + " is not a valid key");
    }
  }
}
