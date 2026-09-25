package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;

final class EventFields {

  static final String STRING_TYPE = "string";
  static final String BOOLEAN_TYPE = "boolean";
  static final String INTEGER_TYPE = "integer";
  static final String OBJECT_TYPE = "object";
  static final String ARRAY_TYPE = "array";
  private static final String MSGTYPE = "msgtype";
  private static final String BODY = "body";
  private static final String FORMATTED_BODY = "formatted_body";
  private static final String FILENAME = "filename";
  private static final String GEO_URI = "geo_uri";
  private static final String URL = "url";
  private static final String FILE = "file";
  private static final String INFO = "info";
  private static final String MENTIONS = "m.mentions";
  private static final String RELATES_TO = "m.relates_to";
  private static final String ROOM = "room";
  private static final String USER_IDS = "user_ids";
  private static final String MIME_TYPE = "mimetype";
  private static final String TYPE_FORMAT = "format";

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
      case STRING_TYPE -> !value.isString();
      case BOOLEAN_TYPE -> !value.isBoolean();
      case INTEGER_TYPE -> !isSafeInteger(value);
      case OBJECT_TYPE -> !value.isObject();
      case ARRAY_TYPE -> !value.isArray();
      default -> true;
    };
  }

  static boolean hasWrongMessageFields(JsonValue content) {
    JsonObject object = content.asObject();
    return hasWrongOptionalMessageFields(object)
        || hasWrongRequiredMessageFields(object)
        || hasWrongMentionFields(object);
  }

  private static boolean hasWrongOptionalMessageFields(JsonObject object) {
    return hasWrongType(object, TYPE_FORMAT, STRING_TYPE)
        || hasWrongType(object, FORMATTED_BODY, STRING_TYPE)
        || hasWrongType(object, FILENAME, STRING_TYPE)
        || hasWrongType(object, GEO_URI, STRING_TYPE)
        || hasWrongType(object, URL, STRING_TYPE)
        || hasWrongType(object, FILE, OBJECT_TYPE)
        || hasWrongType(object, INFO, OBJECT_TYPE)
        || hasWrongType(object, MENTIONS, OBJECT_TYPE)
        || hasWrongType(object, RELATES_TO, OBJECT_TYPE);
  }

  private static boolean hasWrongRequiredMessageFields(JsonObject object) {
    return hasWrongType(object, BODY, STRING_TYPE) || hasWrongType(object, MSGTYPE, STRING_TYPE);
  }

  private static boolean hasWrongMentionFields(JsonObject object) {
    JsonValue mentions = object.get(MENTIONS);
    return mentions != null
        && mentions.isObject()
        && (hasWrongType(mentions.asObject(), ROOM, BOOLEAN_TYPE)
            || hasWrongStringArray(mentions.asObject(), USER_IDS));
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
          || hasWrongType(representation.asObject(), "body", STRING_TYPE)
          || !representation.asObject().has("body")
          || hasWrongType(representation.asObject(), MIME_TYPE, STRING_TYPE)) {
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
