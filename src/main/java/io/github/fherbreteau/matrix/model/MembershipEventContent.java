package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.member} state event, including the original JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroommember">Matrix
 *     specification</a>
 */
public record MembershipEventContent(
    String membership,
    String displayName,
    String avatarUrl,
    Boolean direct,
    String joinAuthorisedViaUsersServer,
    String reason,
    ThirdPartyInvite thirdPartyInvite,
    JsonValue raw)
    implements EventContent {

  private static final String MEMBERSHIP_TYPE = "membership";
  private static final String DISPLAY_NAME = "displayname";
  private static final String AVATAR_URL = "avatar_url";
  private static final String IS_DIRECT = "is_direct";
  private static final String JOIN_AUTHORISED_VIA = "join_authorised_via_users_server";
  private static final String REASON = "reason";
  private static final String THIRD_PARTY_INVITE = "third_party_invite";

  /**
   * Parses membership content while preserving fields this model does not explicitly expose.
   *
   * @param content the raw event content
   * @return the typed content, or {@code null} if required membership data is missing or malformed
   */
  public static MembershipEventContent from(JsonValue content) {
    String membership = EventFields.string(content, MEMBERSHIP_TYPE);
    if (membership == null
        || content == null
        || !content.isObject()
        || EventFields.hasWrongType(content.asObject(), MEMBERSHIP_TYPE, EventFields.STRING_TYPE)
        || hasWrongOptionalFields(content)) {
      return null;
    }
    return new MembershipEventContent(
        membership,
        EventFields.string(content, DISPLAY_NAME),
        EventFields.string(content, AVATAR_URL),
        EventFields.booleanValue(content, IS_DIRECT),
        EventFields.string(content, JOIN_AUTHORISED_VIA),
        EventFields.string(content, REASON),
        ThirdPartyInvite.from(EventFields.field(content, THIRD_PARTY_INVITE)),
        content);
  }

  private static boolean hasWrongOptionalFields(JsonValue content) {
    var object = content.asObject();
    JsonValue displayName = object.get(DISPLAY_NAME);
    JsonValue thirdPartyInvite = object.get(THIRD_PARTY_INVITE);
    return (displayName != null && !displayName.isNull() && !displayName.isString())
        || EventFields.hasWrongType(object, AVATAR_URL, EventFields.STRING_TYPE)
        || EventFields.hasWrongType(object, IS_DIRECT, EventFields.BOOLEAN_TYPE)
        || EventFields.hasWrongType(object, JOIN_AUTHORISED_VIA, EventFields.STRING_TYPE)
        || EventFields.hasWrongType(object, REASON, EventFields.STRING_TYPE)
        || EventFields.hasWrongType(object, THIRD_PARTY_INVITE, EventFields.OBJECT_TYPE);
  }
}
