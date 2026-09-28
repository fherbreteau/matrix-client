package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Typed thumbnail dimensions, MIME type and size with raw JSON retained for extensions.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mimage_imageinfo">Matrix
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mfile_fileinfo">Matrix file info
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mvideo_videoinfo">Matrix video
 *     info specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mlocation_locationinfo">Matrix
 *     location info specification</a>
 */
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
