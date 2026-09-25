package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;

final class EventFields {

  private EventFields() {}

  static JsonValue field(JsonValue content, String name) {
    return content != null && content.isObject() ? content.asObject().get(name) : null;
  }

  static String string(JsonValue content, String name) {
    JsonValue value = field(content, name);
    return value != null && value.isString() ? value.asString() : null;
  }

  static Boolean booleanValue(JsonValue content, String name) {
    JsonValue value = field(content, name);
    return value != null && value.isBoolean() ? value.asBoolean() : null;
  }

  static Long longField(JsonValue content, String name) {
    JsonValue value = field(content, name);
    if (value == null || !value.isNumber()) {
      return null;
    }
    try {
      return value.asBigDecimal().longValueExact();
    } catch (ArithmeticException _) {
      return null;
    }
  }

  static Long longValue(JsonObject content, String name) {
    JsonValue value = content.get(name);
    if (value == null || !value.isNumber()) {
      return null;
    }
    try {
      return value.asBigDecimal().longValueExact();
    } catch (ArithmeticException _) {
      return null;
    }
  }

  static boolean hasWrongType(JsonObject content, String name, String expected) {
    JsonValue value = content.get(name);
    if (value == null || value.isNull()) {
      return false;
    }
    return hasWrongType(value, expected);
  }

  static boolean hasWrongType(JsonValue value, String expected) {
    return switch (expected) {
      case "string" -> !value.isString();
      case "boolean" -> !value.isBoolean();
      case "integer" -> !isSafeInteger(value);
      case "object" -> !value.isObject();
      case "array" -> !value.isArray();
      default -> true;
    };
  }

  static boolean hasWrongMessageFields(JsonValue content) {
    JsonObject object = content.asObject();
    if (hasWrongType(object, "format", "string")
        || hasWrongType(object, "formatted_body", "string")
        || hasWrongType(object, "filename", "string")
        || hasWrongType(object, "geo_uri", "string")
        || hasWrongType(object, "url", "string")
        || hasWrongType(object, "file", "object")
        || hasWrongType(object, "info", "object")
        || hasWrongType(object, "m.mentions", "object")
        || hasWrongType(object, "m.relates_to", "object")) {
      return true;
    }
    if (hasWrongType(object, "body", "string") || hasWrongType(object, "msgtype", "string")) {
      return true;
    }
    JsonValue mentions = object.get("m.mentions");
    return mentions != null
        && mentions.isObject()
        && (hasWrongType(mentions.asObject(), "room", "boolean")
            || hasWrongStringArray(mentions.asObject(), "user_ids"));
  }

  static boolean hasWrongTextRepresentations(JsonValue value) {
    if (!value.isObject()) {
      return true;
    }
    JsonValue text = value.asObject().get("m.text");
    if (text == null) {
      return false;
    }
    if (!text.isArray()) {
      return true;
    }
    for (int i = 0; i < text.asArray().size(); i++) {
      JsonValue representation = text.asArray().get(i);
      if (!representation.isObject()
          || hasWrongType(representation.asObject(), "body", "string")
          || !representation.asObject().has("body")
          || hasWrongType(representation.asObject(), "mimetype", "string")) {
        return true;
      }
    }
    return false;
  }

  static boolean hasWrongStringArray(JsonObject content, String name) {
    JsonValue value = content.get(name);
    if (value == null || value.isNull()) {
      return false;
    }
    if (!value.isArray()) {
      return true;
    }
    for (int i = 0; i < value.asArray().size(); i++) {
      if (!value.asArray().get(i).isString()) {
        return true;
      }
    }
    return false;
  }

  static boolean hasWrongIntegerMap(JsonObject content, String name) {
    JsonValue value = content.get(name);
    if (value == null || value.isNull()) {
      return false;
    }
    if (!value.isObject()) {
      return true;
    }
    for (var entry : value.asObject().entrySet()) {
      if (!isSafeInteger(entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSafeInteger(JsonValue value) {
    if (!value.isNumber()) {
      return false;
    }
    try {
      BigDecimal number = value.asBigDecimal();
      BigInteger integer = number.toBigIntegerExact();
      return integer.abs().compareTo(BigInteger.valueOf(9_007_199_254_740_991L)) <= 0;
    } catch (ArithmeticException _) {
      return false;
    }
  }
}
