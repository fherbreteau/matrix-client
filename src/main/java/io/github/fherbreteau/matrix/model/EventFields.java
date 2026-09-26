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
  static final String MESSAGE_TYPE = "msgtype";
  static final String REPRESENTATION_BODY = "body";
  static final String BODY = "body";
  static final String TOPIC = "topic";
  static final String NAME = "name";
  static final String TEXT_REPRESENTATIONS = "m.text";
  private static final String FORMATTED_BODY = "formatted_body";
  private static final String FILENAME = "filename";
  private static final String GEO_URI = "geo_uri";
  private static final String URL = "url";
  private static final String FILE = "file";
  private static final String INFO = "info";
  private static final String THUMBNAIL_INFO = "thumbnail_info";
  private static final String MENTIONS = "m.mentions";
  private static final String RELATES_TO = "m.relates_to";
  private static final String ROOM = "room";
  private static final String USER_IDS = "user_ids";
  static final String MIME_TYPE = "mimetype";
  static final String TYPE_FORMAT = "format";
  private static final String THUMBNAIL_URL = "thumbnail_url";
  private static final String THUMBNAIL_FILE = "thumbnail_file";
  private static final String ENCRYPTED_KEY = "key";
  private static final String ENCRYPTED_HASHES = "hashes";
  private static final String ENCRYPTED_IV = "iv";
  private static final String ENCRYPTED_URL = "url";
  private static final String ENCRYPTED_VERSION = "v";
  private static final String ENCRYPTED_ALGORITHM = "alg";
  private static final String ENCRYPTED_EXTENDED = "ext";
  private static final String ENCRYPTED_KEY_DATA = "k";
  private static final String ENCRYPTED_KEY_OPERATIONS = "key_ops";
  private static final String ENCRYPTED_KEY_TYPE = "kty";
  private static final String ANIMATED = "is_animated";
  private static final String HEIGHT = "h";
  private static final String WIDTH = "w";
  private static final String DURATION = "duration";
  private static final String SIZE = "size";

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

  static boolean hasWrongEncryptedFileShape(JsonValue value) {
    return value != null && !value.isNull() && EncryptedMediaFile.from(value) == null;
  }

  static boolean hasWrongMessageInfoFields(JsonValue info) {
    JsonObject object = info.asObject();
    return hasWrongIntegerField(object, HEIGHT)
        || hasWrongIntegerField(object, WIDTH)
        || hasWrongIntegerField(object, DURATION)
        || hasWrongIntegerField(object, SIZE)
        || hasWrongType(object, MIME_TYPE, STRING_TYPE)
        || hasWrongType(object, ANIMATED, BOOLEAN_TYPE)
        || hasWrongType(object, THUMBNAIL_URL, STRING_TYPE)
        || hasWrongEncryptedFileShape(object.get(THUMBNAIL_FILE))
        || hasWrongThumbnailInfoField(object);
  }

  static boolean hasWrongThumbnailInfoFields(JsonValue info) {
    JsonObject object = info.asObject();
    return hasWrongIntegerField(object, HEIGHT)
        || hasWrongIntegerField(object, WIDTH)
        || hasWrongIntegerField(object, SIZE)
        || hasWrongType(object, MIME_TYPE, STRING_TYPE);
  }

  static boolean hasWrongIntegerField(JsonObject object, String name) {
    return hasWrongType(object, name, INTEGER_TYPE);
  }

  private static boolean hasWrongThumbnailInfoField(JsonObject object) {
    JsonValue thumbnailInfo = object.get(THUMBNAIL_INFO);
    return thumbnailInfo != null
        && !thumbnailInfo.isNull()
        && (!thumbnailInfo.isObject() || hasWrongThumbnailInfoFields(thumbnailInfo));
  }

  static boolean hasWrongMessageFields(JsonValue content) {
    JsonObject object = content.asObject();
    return hasWrongOptionalMessageFields(object)
        || hasWrongRequiredMessageFields(object)
        || hasWrongMentionFields(object)
        || hasWrongFileField(object)
        || hasWrongInfoField(object);
  }

  private static boolean hasWrongFileField(JsonObject content) {
    JsonValue file = content.get(FILE);
    return file != null && !file.isNull() && !file.isObject();
  }

  private static boolean hasWrongInfoField(JsonObject content) {
    JsonValue info = content.get(INFO);
    return info != null && !info.isNull() && (!info.isObject() || hasWrongMessageInfoFields(info));
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
    return !object.has(BODY)
        || hasWrongType(object, BODY, STRING_TYPE)
        || !object.has(MESSAGE_TYPE)
        || hasWrongType(object, MESSAGE_TYPE, STRING_TYPE);
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
    JsonValue text = value.asObject().get(TEXT_REPRESENTATIONS);
    if (text == null) {
      return false;
    }
    if (!text.isArray()) {
      return true;
    }
    for (int i = 0; i < text.asArray().size(); i++) {
      JsonValue representation = text.asArray().get(i);
      if (!representation.isObject()
          || hasWrongType(representation.asObject(), REPRESENTATION_BODY, STRING_TYPE)
          || !representation.asObject().has(REPRESENTATION_BODY)
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

  static java.util.List<String> stringList(JsonValue value) {
    if (value == null || !value.isArray()) {
      return null;
    }
    var strings = new java.util.ArrayList<String>();
    for (int i = 0; i < value.asArray().size(); i++) {
      JsonValue item = value.asArray().get(i);
      if (!item.isString()) {
        return null;
      }
      strings.add(item.asString());
    }
    return java.util.List.copyOf(strings);
  }

  static java.util.Map<String, String> stringMap(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    var strings = new java.util.LinkedHashMap<String, String>();
    for (var entry : value.asObject().entrySet()) {
      if (!entry.getValue().isString()) {
        return null;
      }
      strings.put(entry.getKey(), entry.getValue().asString());
    }
    return java.util.Collections.unmodifiableMap(strings);
  }

  static boolean hasWrongIntegerObject(JsonValue value) {
    if (value == null || value.isNull()) {
      return false;
    }
    return hasWrongIntegerMap(value);
  }

  static boolean hasWrongIntegerMap(JsonObject content, String name) {
    JsonValue value = content.get(name);
    return value != null && !value.isNull() && hasWrongIntegerMap(value);
  }

  static boolean hasWrongIntegerMap(JsonValue value) {
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

  static java.util.Map<String, Long> integerMap(JsonValue value) {
    if (value == null || value.isNull()) {
      return java.util.Map.of();
    }
    if (hasWrongIntegerMap(value)) {
      return null;
    }
    var result = new java.util.LinkedHashMap<String, Long>();
    for (var entry : value.asObject().entrySet()) {
      result.put(entry.getKey(), entry.getValue().asBigDecimal().longValueExact());
    }
    return java.util.Collections.unmodifiableMap(result);
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
