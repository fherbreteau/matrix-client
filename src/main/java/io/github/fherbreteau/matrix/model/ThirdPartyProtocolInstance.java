package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * One configured instance of a third-party protocol.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyprotocolprotocol">Matrix
 *     specification</a>
 */
public record ThirdPartyProtocolInstance(
    String description,
    JsonValue fields,
    String icon,
    String instanceId,
    String networkId,
    JsonValue raw) {

  private static final String MODEL_NAME = "third-party protocol instance";

  /**
   * Parses a protocol instance.
   *
   * @param value the protocol instance JSON
   * @return the parsed protocol instance
   * @throws IllegalArgumentException if required fields are missing or malformed
   */
  public static ThirdPartyProtocolInstance from(JsonValue value) {
    JsonObject object = ModelJson.object(value, MODEL_NAME);
    return new ThirdPartyProtocolInstance(
        ModelJson.requiredString(object, "desc", MODEL_NAME),
        requiredObject(object, "fields"),
        ModelJson.string(object, "icon"),
        ModelJson.string(object, "instance_id"),
        ModelJson.requiredString(object, "network_id", MODEL_NAME),
        value);
  }

  private static JsonValue requiredObject(JsonObject object, String field) {
    JsonValue value = object.get(field);
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException(MODEL_NAME + " must contain " + field);
    }
    return value;
  }
}
