package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;

/** Parsed Matrix encrypted-file descriptor, retaining the original JSON representation. */
public record EncryptedMediaFile(
    Map<String, String> hashes,
    String initializationVector,
    EncryptedMediaKey key,
    String contentUri,
    String version,
    JsonValue raw) {

  static EncryptedMediaFile from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    var object = value.asObject();
    var hashes = EventFields.stringMap(object.get("hashes"));
    EncryptedMediaKey key = EncryptedMediaKey.from(object.get("key"));
    String iv = EventFields.string(value, "iv");
    String url = EventFields.string(value, "url");
    String version = EventFields.string(value, "v");
    if (hashes == null || key == null || iv == null || url == null || version == null) {
      return null;
    }
    if (!"v2".equals(version)) {
      return null;
    }
    return new EncryptedMediaFile(hashes, iv, key, url, version, value);
  }
}
