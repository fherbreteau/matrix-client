package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A third-party network location mapped to a Matrix room alias.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartylocation">Matrix
 *     specification</a>
 */
public record ThirdPartyLocation(
    RoomAlias alias, JsonValue fields, String protocol, JsonValue raw) {

  private static final String DESCRIPTION = "third-party location";

  /**
   * Parses a third-party location.
   *
   * @param value the location JSON
   * @return the parsed location
   * @throws IllegalArgumentException if required fields are missing or malformed
   */
  public static ThirdPartyLocation from(JsonValue value) {
    JsonObject object = ModelJson.object(value, DESCRIPTION);
    JsonValue fields = object.get("fields");
    if (fields == null || !fields.isObject()) {
      throw new IllegalArgumentException(DESCRIPTION + " must contain fields object");
    }
    return new ThirdPartyLocation(
        RoomAlias.of(ModelJson.requiredString(object, "alias", DESCRIPTION)),
        fields,
        ModelJson.requiredString(object, "protocol", DESCRIPTION),
        value);
  }
}
