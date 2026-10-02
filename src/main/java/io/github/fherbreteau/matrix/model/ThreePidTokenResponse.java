package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Response from a registration or third-party identifier verification token request.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3registeremailrequesttoken">Matrix
 *     specification</a>
 */
public record ThreePidTokenResponse(String sid, String submitUrl, JsonValue raw) {

  /** Parses a successful third-party identifier token response. */
  public static ThreePidTokenResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("Token response must be a JSON object");
    }
    JsonValue sid = value.asObject().get("sid");
    JsonValue submitUrl = value.asObject().get("submit_url");
    if (sid == null || !sid.isString()) {
      throw new IllegalArgumentException("Token response must contain sid");
    }
    return new ThreePidTokenResponse(
        sid.asString(),
        submitUrl != null && submitUrl.isString() ? submitUrl.asString() : null,
        value);
  }
}
