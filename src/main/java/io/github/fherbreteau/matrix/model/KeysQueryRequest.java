package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request body for querying device keys.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysquery">Matrix
 *     specification</a>
 */
public record KeysQueryRequest(Map<UserId, List<String>> deviceKeys, Long timeout)
    implements Serializable {
  /**
   * Creates a query request with copied user and device lists.
   *
   * @param deviceKeys user IDs mapped to requested device IDs; an empty list means all devices
   * @param timeout optional remote query timeout in milliseconds
   */
  public KeysQueryRequest {
    Objects.requireNonNull(deviceKeys, "deviceKeys");
    deviceKeys = immutableMap(deviceKeys);
  }

  @Override
  public JsonValue toJson() {
    JsonObject request = new JsonObject();
    request.put("device_keys", ModelJson.toObject(deviceKeys));
    if (timeout != null) {
      request.put("timeout", timeout);
    }
    return request;
  }
}
