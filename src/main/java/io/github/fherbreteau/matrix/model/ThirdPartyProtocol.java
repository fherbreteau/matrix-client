package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata describing a third-party network protocol.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyprotocolprotocol">Matrix
 *     specification</a>
 */
public record ThirdPartyProtocol(
    Map<String, ThirdPartyFieldType> fieldTypes,
    String icon,
    List<ThirdPartyProtocolInstance> instances,
    List<String> locationFields,
    List<String> userFields,
    JsonValue raw) {

  /** Copies collection fields into immutable ordered collections. */
  public ThirdPartyProtocol {
    fieldTypes = Collections.unmodifiableMap(new LinkedHashMap<>(fieldTypes));
    instances = List.copyOf(instances);
    locationFields = List.copyOf(locationFields);
    userFields = List.copyOf(userFields);
  }

  /**
   * Parses protocol metadata.
   *
   * @param value the protocol JSON
   * @return the parsed protocol
   * @throws IllegalArgumentException if required fields are missing or malformed
   */
  public static ThirdPartyProtocol from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "third-party protocol");
    JsonValue fieldTypesValue = object.get("field_types");
    if (fieldTypesValue == null || !fieldTypesValue.isObject()) {
      throw new IllegalArgumentException("third-party protocol must contain field_types object");
    }
    Map<String, ThirdPartyFieldType> fieldTypes = new LinkedHashMap<>();
    for (Map.Entry<String, JsonValue> entry : fieldTypesValue.asObject().entrySet()) {
      fieldTypes.put(entry.getKey(), ThirdPartyFieldType.from(entry.getValue()));
    }
    JsonValue instancesValue = object.get("instances");
    if (instancesValue == null || !instancesValue.isArray()) {
      throw new IllegalArgumentException("third-party protocol must contain instances array");
    }
    List<ThirdPartyProtocolInstance> instances = new java.util.ArrayList<>();
    JsonArray instancesArray = instancesValue.asArray();
    for (int index = 0; index < instancesArray.size(); index++) {
      instances.add(ThirdPartyProtocolInstance.from(instancesArray.get(index)));
    }
    String icon = ModelJson.requiredString(object, "icon", "third-party protocol");
    List<String> locationFields = requiredStrings(object, "location_fields");
    List<String> userFields = requiredStrings(object, "user_fields");
    return new ThirdPartyProtocol(fieldTypes, icon, instances, locationFields, userFields, value);
  }

  private static List<String> requiredStrings(JsonObject object, String field) {
    JsonValue value = object.get(field);
    if (value == null || !value.isArray()) {
      throw new IllegalArgumentException("third-party protocol must contain " + field + " array");
    }
    return ModelJson.strings(value, "third-party protocol " + field);
  }
}
