package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/** Typed thumbnail dimensions, MIME type and size with raw JSON retained for extensions. */
public record ThumbnailInfo(Long height, Long width, Long size, String mimeType, JsonValue raw) {

  static ThumbnailInfo from(JsonValue value) {
    if (value == null || !value.isObject() || EventFields.hasWrongThumbnailInfoFields(value)) {
      return null;
    }
    return new ThumbnailInfo(
        EventFields.longField(value, "h"),
        EventFields.longField(value, "w"),
        EventFields.longField(value, "size"),
        EventFields.string(value, "mimetype"),
        value);
  }
}
