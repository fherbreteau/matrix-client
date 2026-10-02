package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Optional device metadata to update.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#put_matrixclientv3devicesdeviceid">Matrix
 *     specification</a>
 */
public record DeviceUpdateRequest(JsonObject body) {

  /** Creates a request body. */
  public DeviceUpdateRequest {
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
  }

  /** Creates a request that changes the display name when the argument is non-null. */
  public static DeviceUpdateRequest displayName(String displayName) {
    JsonObject body = new JsonObject();
    if (displayName != null) {
      body.put("display_name", displayName);
    }
    return new DeviceUpdateRequest(body);
  }

  /** Serializes this request body. */
  public JsonValue toJson() {
    return body;
  }
}
