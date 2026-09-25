package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.member} state event, including the original JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroommember">Matrix
 *     specification</a>
 */
public record MembershipEventContent(
    String membership, String displayName, String avatarUrl, String reason, JsonValue raw)
    implements EventContent {

  /** Parses membership fields while retaining unknown fields. */
  public static MembershipEventContent from(JsonValue content) {
    return new MembershipEventContent(
        EventFields.string(content, "membership"),
        EventFields.string(content, "displayname"),
        EventFields.string(content, "avatar_url"),
        EventFields.string(content, "reason"),
        content);
  }
}
