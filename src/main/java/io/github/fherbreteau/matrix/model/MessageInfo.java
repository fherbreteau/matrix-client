package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/** Typed common media metadata in a message content {@code info} object. */
public record MessageInfo(
    Long height,
    Long width,
    Long duration,
    Long size,
    String mimeType,
    Boolean animated,
    String thumbnailUrl,
    JsonValue thumbnailFile,
    ThumbnailInfo thumbnailInfo,
    JsonValue raw) {

  static MessageInfo from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    JsonValue thumbnailInfoValue = EventFields.field(value, "thumbnail_info");
    return new MessageInfo(
        EventFields.longField(value, "h"),
        EventFields.longField(value, "w"),
        EventFields.longField(value, "duration"),
        EventFields.longField(value, "size"),
        EventFields.string(value, "mimetype"),
        EventFields.booleanValue(value, "is_animated"),
        EventFields.string(value, "thumbnail_url"),
        EventFields.field(value, "thumbnail_file"),
        ThumbnailInfo.from(thumbnailInfoValue),
        value);
  }
}
