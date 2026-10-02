package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonString;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Request body for bulk device deletion, optionally carrying UI-auth data.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3delete_devices">Matrix
 *     specification</a>
 */
public record DeleteDevicesRequest(JsonObject body) {

  /** Creates a request body. */
  public DeleteDevicesRequest {
    if (body == null || !body.has("devices")) {
      throw new IllegalArgumentException("devices are required");
    }
  }

  /** Creates a request for the device identifiers. */
  public static DeleteDevicesRequest of(Iterable<DeviceId> deviceIds) {
    JsonArray ids = new JsonArray();
    for (DeviceId deviceId : deviceIds) {
      ids.add(JsonString.of(deviceId.value()));
    }
    return new DeleteDevicesRequest(new JsonObject().put("devices", ids));
  }

  /** Returns a copy with the UI-auth response added. */
  public DeleteDevicesRequest withAuth(JsonValue auth) {
    JsonObject copy = new JsonObject();
    body.entrySet().forEach(entry -> copy.put(entry.getKey(), entry.getValue()));
    copy.put("auth", auth);
    return new DeleteDevicesRequest(copy);
  }

  /** Serializes this request body. */
  public JsonValue toJson() {
    return body;
  }
}
