package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Counts of unclaimed one-time keys after upload.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keysupload">Matrix
 *     specification</a>
 */
public record KeysUploadResponse(Map<String, Long> oneTimeKeyCounts) {
  /**
   * Creates the parsed key counts response.
   *
   * @param oneTimeKeyCounts counts by key algorithm
   */
  public KeysUploadResponse {
    oneTimeKeyCounts = Map.copyOf(oneTimeKeyCounts);
  }

  /**
   * Validate and parse a (/key/upload) response body.
   *
   * @param body the parsed response body
   * @return the validated versions and features
   * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
   *     versions} array of strings
   */
  public static KeysUploadResponse from(JsonValue body) {
    var response = ModelJson.object(body, "response");
    var extracted = ModelJson.requiredObject(response, "one_time_key_counts", "response");
    Map<String, Long> oneTimeKeyCounts = longMap(extracted);
    return new KeysUploadResponse(oneTimeKeyCounts);
  }

  private static Map<String, Long> longMap(JsonObject source) {
    Map<String, Long> result = new LinkedHashMap<>();
    source
        .entrySet()
        .forEach(
            entry ->
                result.put(entry.getKey(), ModelJson.number(entry.getValue(), entry.getKey())));
    return result;
  }
}
