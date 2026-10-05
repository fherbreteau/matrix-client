package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A third-party user mapped to a Matrix user ID.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyuser">Matrix
 *     specification</a>
 */
public record ThirdPartyUser(JsonValue fields, String protocol, UserId userId, JsonValue raw) {

  private static final String DESCRIPTION = "third-party user";

  /**
   * Parses a third-party user.
   *
   * @param value the user JSON
   * @return the parsed third-party user
   * @throws IllegalArgumentException if required fields are missing or malformed
   */
  public static ThirdPartyUser from(JsonValue value) {
    JsonObject object = ModelJson.object(value, DESCRIPTION);
    JsonValue fields = object.get("fields");
    if (fields == null || !fields.isObject()) {
      throw new IllegalArgumentException(DESCRIPTION + " must contain fields object");
    }
    return new ThirdPartyUser(
        fields,
        ModelJson.requiredString(object, "protocol", DESCRIPTION),
        UserId.of(ModelJson.requiredString(object, "userid", DESCRIPTION)),
        value);
  }
}
