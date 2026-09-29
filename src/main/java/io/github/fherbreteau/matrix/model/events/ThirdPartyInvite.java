package io.github.fherbreteau.matrix.model.events;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Typed {@code third_party_invite} member-event content with signature data preserved as JSON.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#mroommember_thirdpartyinvite">Matrix
 *     specification</a>
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#mroommember_thirdpartyinvite">Matrix
 *     signature specification</a>
 */
public record ThirdPartyInvite(
    String displayName, String matrixUserId, String token, JsonValue signatures, JsonValue raw) {

  static ThirdPartyInvite from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    JsonValue signed = EventFields.field(value, "signed");
    return new ThirdPartyInvite(
        EventFields.string(value, "display_name"),
        EventFields.string(signed, "mxid"),
        EventFields.string(signed, "token"),
        EventFields.field(signed, "signatures"),
        value);
  }
}
