package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Devices registered for the current Matrix user, retaining the raw response.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3devices">Matrix
 *     specification</a>
 */
public record DevicesResponse(List<Device> devices, JsonValue raw) {

  /** Creates a response with an immutable list of devices. */
  public DevicesResponse {
    devices = List.copyOf(devices);
  }

  /** Parses the device-list response. */
  public static DevicesResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new DiscoveryException("Devices response must be a JSON object");
    }
    JsonValue devices = value.asObject().get("devices");
    if (devices == null || !devices.isArray()) {
      throw new DiscoveryException("Devices response must contain devices");
    }
    JsonArray array = devices.asArray();
    List<Device> parsed = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      parsed.add(Device.from(array.get(index)));
    }
    return new DevicesResponse(parsed, value);
  }
}
