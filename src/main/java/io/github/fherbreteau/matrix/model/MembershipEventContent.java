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

  /**
   * Parses membership content while preserving fields this model does not explicitly expose.
   *
   * @param content the raw event content
   * @return the typed content, or {@code null} if required membership data is missing or malformed
   */
  public static MembershipEventContent from(JsonValue content) {
    String membership = EventFields.string(content, "membership");
    if (membership == null
        || content == null
        || !content.isObject()
        || EventFields.hasWrongType(content.asObject(), "membership", "string")
        || hasWrongOptionalFields(content)) {
      return null;
    }
    return new MembershipEventContent(
        membership,
        EventFields.string(content, "displayname"),
        EventFields.string(content, "avatar_url"),
        EventFields.booleanValue(content, "is_direct"),
        EventFields.string(content, "join_authorised_via_users_server"),
        EventFields.string(content, "reason"),
        ThirdPartyInvite.from(EventFields.field(content, "third_party_invite")),
        content);
  }

  private static boolean hasWrongOptionalFields(JsonValue content) {
    var object = content.asObject();
    JsonValue displayName = object.get("displayname");
    JsonValue thirdPartyInvite = object.get("third_party_invite");
    return (displayName != null && !displayName.isNull() && !displayName.isString())
        || EventFields.hasWrongType(object, "avatar_url", "string")
        || EventFields.hasWrongType(object, "is_direct", "boolean")
        || EventFields.hasWrongType(object, "join_authorised_via_users_server", "string")
        || EventFields.hasWrongType(object, "reason", "string")
        || (thirdPartyInvite != null && !thirdPartyInvite.isNull() && !thirdPartyInvite.isObject());
  }
}
