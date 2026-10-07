package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Map;
import java.util.Objects;

/**
 * Request body for claiming one-time or fallback keys.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysclaim">Matrix
 *     specification</a>
 */
public record KeysClaimRequest(Map<UserId, Map<String, String>> oneTimeKeys, Long timeout)
    implements Serializable {
  /**
   * Creates a claim request with a defensive copy of its target map.
   *
   * @param oneTimeKeys user/device IDs mapped to requested algorithms
   * @param timeout optional remote claim timeout in milliseconds
   */
  public KeysClaimRequest {
    Objects.requireNonNull(oneTimeKeys, "oneTimeKeys");
    oneTimeKeys = immutableMap(oneTimeKeys);
  }

  @Override
  public JsonValue toJson() {
    JsonObject request = new JsonObject();
    request.put("one_time_keys", ModelJson.toObject(oneTimeKeys));
    if (timeout != null) {
      request.put("timeout", timeout);
    }
    return request;
  }
}
