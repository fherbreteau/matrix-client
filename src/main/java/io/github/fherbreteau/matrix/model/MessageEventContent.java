package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.message} event, with the original JSON retained.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroommessage-msgtypes">Matrix
 *     specification</a>
 */
public record MessageEventContent(
    String msgtype, String body, String format, String formattedBody, JsonValue raw)
    implements EventContent {

  /** Parses an {@code m.room.message} content object without rejecting additional fields. */
  public static MessageEventContent from(JsonValue content) {
    return new MessageEventContent(
        EventFields.string(content, "msgtype"),
        EventFields.string(content, "body"),
        EventFields.string(content, "format"),
        EventFields.string(content, "formatted_body"),
        content);
  }
}
