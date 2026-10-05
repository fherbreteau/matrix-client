package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Field validation metadata for a third-party protocol.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyprotocolprotocol">Matrix
 *     specification</a>
 */
public record ThirdPartyFieldType(String placeholder, String regexp, JsonValue raw) {

  /**
   * Parses field type metadata.
   *
   * @param value the field type JSON
   * @return the parsed field type
   * @throws IllegalArgumentException if required fields are missing or malformed
   */
  public static ThirdPartyFieldType from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "third-party field type");
    return new ThirdPartyFieldType(
        ModelJson.requiredString(object, "placeholder", "third-party field type"),
        ModelJson.requiredString(object, "regexp", "third-party field type"),
        value);
  }
}
