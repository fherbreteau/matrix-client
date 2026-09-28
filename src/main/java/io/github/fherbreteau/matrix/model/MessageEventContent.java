package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * Typed content of an {@code m.room.message} event, selected by its {@code msgtype}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroommessage-msgtypes">Matrix
 *     specification</a>
 */
public sealed interface MessageEventContent extends EventContent
    permits MessageEventContent.Text,
        MessageEventContent.Emote,
        MessageEventContent.Notice,
        MessageEventContent.Image,
        MessageEventContent.File,
        MessageEventContent.Audio,
        MessageEventContent.Video,
        MessageEventContent.Location,
        MessageEventContent.VerificationRequest,
        MessageEventContent.Unknown {

  String TEXT_TYPE = "m.text";
  String EMOTE_TYPE = "m.emote";
  String NOTICE_TYPE = "m.notice";
  String IMAGE_TYPE = "m.image";
  String FILE_TYPE = "m.file";
  String AUDIO_TYPE = "m.audio";
  String VIDEO_TYPE = "m.video";
  String LOCATION_TYPE = "m.location";
  String VERIFICATION_REQUEST_TYPE = "m.key.verification.request";

  /**
   * Returns this message's {@code msgtype}.
   *
   * @return the message type
   */
  String msgtype();

  /**
   * Returns the plain-text body.
   *
   * @return the body text
   */
  String body();

  /**
   * Returns the formatting identifier, or {@code null} when absent.
   *
   * @return the format identifier
   */
  String format();

  /**
   * Returns the formatted body, or {@code null} when absent.
   *
   * @return the formatted body
   */
  String formattedBody();

  /**
   * Returns typed mentions, or {@code null} when absent.
   *
   * @return typed mentions
   */
  MessageMentions mentions();

  /**
   * Returns the event relation, or {@code null} when absent.
   *
   * @return the event relation
   */
  EventRelation relatesTo();

  /**
   * Returns the complete original content JSON.
   *
   * @return raw content
   */
  JsonValue raw();

  /** Text message fields. */
  record Text(
      String body,
      String format,
      String formattedBody,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return TEXT_TYPE;
    }
  }

  /** Emote message fields. */
  record Emote(
      String body,
      String format,
      String formattedBody,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return EMOTE_TYPE;
    }
  }

  /** Notice message fields. */
  record Notice(
      String body,
      String format,
      String formattedBody,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return NOTICE_TYPE;
    }
  }

  /** Image message fields. */
  record Image(
      String body,
      String format,
      String formattedBody,
      String filename,
      MessageMediaSource source,
      MessageInfo info,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return IMAGE_TYPE;
    }
  }

  /** File message fields. */
  record File(
      String body,
      String format,
      String formattedBody,
      String filename,
      MessageMediaSource source,
      MessageInfo info,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return FILE_TYPE;
    }
  }

  /** Audio message fields. */
  record Audio(
      String body,
      String format,
      String formattedBody,
      String filename,
      MessageMediaSource source,
      MessageInfo info,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return AUDIO_TYPE;
    }
  }

  /** Video message fields. */
  record Video(
      String body,
      String format,
      String formattedBody,
      String filename,
      MessageMediaSource source,
      MessageInfo info,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return VIDEO_TYPE;
    }
  }

  /** Location message fields, including the required geographic URI. */
  record Location(
      String body,
      String format,
      String formattedBody,
      String geoUri,
      MessageInfo info,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return LOCATION_TYPE;
    }
  }

  /** In-room key verification request message fields. */
  record VerificationRequest(
      String body,
      String format,
      String formattedBody,
      String fromDevice,
      String to,
      List<String> methods,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {

    @Override
    public String msgtype() {
      return VERIFICATION_REQUEST_TYPE;
    }
  }

  /** Unknown or custom message type, with common fields and the complete raw content. */
  record Unknown(
      String msgtype,
      String body,
      String format,
      String formattedBody,
      MessageMentions mentions,
      EventRelation relatesTo,
      JsonValue raw)
      implements MessageEventContent {}

  /**
   * Parses message content into its subtype selected by {@code msgtype}.
   *
   * @param content raw message content object
   * @return the matching subtype, or {@code null} when required data is malformed
   */
  static MessageEventContent from(JsonValue content) {
    if (content == null || !content.isObject()) {
      return null;
    }
    var object = content.asObject();
    String msgtype = EventFields.string(content, EventFields.MESSAGE_TYPE);
    String body = EventFields.string(content, EventFields.BODY);
    if (msgtype == null
        || body == null
        || !object.has(EventFields.MESSAGE_TYPE)
        || !object.has(EventFields.BODY)
        || EventFields.hasWrongMessageFields(content)) {
      return null;
    }
    String format = EventFields.string(content, EventFields.TYPE_FORMAT);
    String formattedBody = EventFields.string(content, "formatted_body");
    MessageMentions mentions = MessageMentions.from(EventFields.field(content, "m.mentions"));
    EventRelation relatesTo = EventRelation.from(EventFields.field(content, "m.relates_to"));
    return switch (msgtype) {
      case TEXT_TYPE -> new Text(body, format, formattedBody, mentions, relatesTo, content);
      case EMOTE_TYPE -> new Emote(body, format, formattedBody, mentions, relatesTo, content);
      case NOTICE_TYPE -> new Notice(body, format, formattedBody, mentions, relatesTo, content);
      case IMAGE_TYPE, FILE_TYPE, AUDIO_TYPE, VIDEO_TYPE ->
          mediaMessage(msgtype, content, body, format, formattedBody, mentions, relatesTo);
      case LOCATION_TYPE -> {
        String geoUri = EventFields.string(content, "geo_uri");
        if (geoUri == null || !object.has("geo_uri")) {
          yield null;
        }
        yield new Location(
            body,
            format,
            formattedBody,
            geoUri,
            MessageInfo.from(EventFields.field(content, "info")),
            mentions,
            relatesTo,
            content);
      }
      case VERIFICATION_REQUEST_TYPE ->
          verificationRequest(content, body, format, formattedBody, mentions, relatesTo);
      default -> new Unknown(msgtype, body, format, formattedBody, mentions, relatesTo, content);
    };
  }

  private static MessageEventContent mediaMessage(
      String msgtype,
      JsonValue content,
      String body,
      String format,
      String formattedBody,
      MessageMentions mentions,
      EventRelation relatesTo) {
    MessageMediaSource source = mediaSource(content);
    if (source == null) {
      return null;
    }
    String filename = EventFields.string(content, "filename");
    MessageInfo info = MessageInfo.from(EventFields.field(content, "info"));
    return switch (msgtype) {
      case IMAGE_TYPE ->
          new Image(
              body, format, formattedBody, filename, source, info, mentions, relatesTo, content);
      case FILE_TYPE ->
          new File(
              body, format, formattedBody, filename, source, info, mentions, relatesTo, content);
      case AUDIO_TYPE ->
          new Audio(
              body, format, formattedBody, filename, source, info, mentions, relatesTo, content);
      case VIDEO_TYPE ->
          new Video(
              body, format, formattedBody, filename, source, info, mentions, relatesTo, content);
      default -> throw new IllegalArgumentException("unsupported media message type");
    };
  }

  private static MessageMediaSource mediaSource(JsonValue content) {
    JsonValue encryptedValue = EventFields.field(content, "file");
    if (encryptedValue != null && !encryptedValue.isNull()) {
      EncryptedMediaFile file = EncryptedMediaFile.from(encryptedValue);
      return file == null ? null : new EncryptedMediaSource(file);
    }
    String url = EventFields.string(content, "url");
    return url == null ? null : new PlainMediaSource(url);
  }

  private static MessageEventContent verificationRequest(
      JsonValue content,
      String body,
      String format,
      String formattedBody,
      MessageMentions mentions,
      EventRelation relatesTo) {
    String fromDevice = EventFields.string(content, "from_device");
    String to = EventFields.string(content, "to");
    JsonValue methodsValue = EventFields.field(content, "methods");
    var methods = EventFields.stringList(methodsValue);
    if (fromDevice == null
        || to == null
        || !content.asObject().has("from_device")
        || !content.asObject().has("to")
        || EventFields.hasWrongStringArray(content.asObject(), "methods")) {
      return null;
    }
    return new VerificationRequest(
        body, format, formattedBody, fromDevice, to, methods, mentions, relatesTo, content);
  }
}
