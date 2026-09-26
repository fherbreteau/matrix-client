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
    EncryptedMediaFile thumbnailFile,
    ThumbnailInfo thumbnailInfo,
    JsonValue raw) {

  static MessageInfo from(JsonValue value) {
    if (value == null || !value.isObject() || EventFields.hasWrongMessageInfoFields(value)) {
      return null;
    }
    JsonValue thumbnailInfoValue = EventFields.field(value, "thumbnail_info");
    if (thumbnailInfoValue != null
        && !thumbnailInfoValue.isNull()
        && !thumbnailInfoValue.isObject()) {
      return null;
    }
    ThumbnailInfo thumbnailInfo = ThumbnailInfo.from(thumbnailInfoValue);
    if (thumbnailInfoValue != null && !thumbnailInfoValue.isNull() && thumbnailInfo == null) {
      return null;
    }
    return new MessageInfo(
        EventFields.longField(value, "h"),
        EventFields.longField(value, "w"),
        EventFields.longField(value, "duration"),
        EventFields.longField(value, "size"),
        EventFields.string(value, "mimetype"),
        EventFields.booleanValue(value, "is_animated"),
        EventFields.string(value, "thumbnail_url"),
        EncryptedMediaFile.from(EventFields.field(value, "thumbnail_file")),
        thumbnailInfo,
        value);
  }
}
