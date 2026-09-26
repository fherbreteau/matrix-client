package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.message} event, with the original JSON retained.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroommessage-msgtypes">Matrix
 *     specification</a>
 */
public record MessageEventContent(
    String msgtype,
    String body,
    String format,
    String formattedBody,
    String filename,
    String url,
    EncryptedMediaFile file,
    MessageInfo info,
    String geoUri,
    MessageMentions mentions,
    EventRelation relatesTo,
    JsonValue raw)
    implements EventContent {

  /**
   * Parses the common and type-specific message fields without discarding extension fields.
   *
   * @param content the raw event content
   * @return the typed content, or {@code null} if the required fields are malformed
   */
  public static MessageEventContent from(JsonValue content) {
    String msgtype = EventFields.string(content, EventFields.MESSAGE_TYPE);
    String body = EventFields.string(content, EventFields.BODY);
    if (content == null
        || !content.isObject()
        || msgtype == null
        || body == null
        || EventFields.hasWrongMessageFields(content)
        || EventFields.hasWrongEncryptedFileShape(EventFields.field(content, "file"))) {
      return null;
    }
    return new MessageEventContent(
        msgtype,
        body,
        EventFields.string(content, EventFields.TYPE_FORMAT),
        EventFields.string(content, "formatted_body"),
        EventFields.string(content, "filename"),
        EventFields.string(content, "url"),
        EncryptedMediaFile.from(EventFields.field(content, "file")),
        MessageInfo.from(EventFields.field(content, "info")),
        EventFields.string(content, "geo_uri"),
        MessageMentions.from(EventFields.field(content, "m.mentions")),
        EventRelation.from(EventFields.field(content, "m.relates_to")),
        content);
  }
}
