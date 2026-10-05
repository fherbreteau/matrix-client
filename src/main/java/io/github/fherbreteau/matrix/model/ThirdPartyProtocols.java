package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Protocol metadata advertised by a homeserver.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartyprotocols">Matrix
 *     specification</a>
 */
public record ThirdPartyProtocols(Map<String, ThirdPartyProtocol> protocols, JsonValue raw) {

  /** Copies protocols into an immutable insertion-ordered map. */
  public ThirdPartyProtocols {
    protocols = Collections.unmodifiableMap(new LinkedHashMap<>(protocols));
  }

  /**
   * Parses the protocol map response.
   *
   * @param value the response JSON
   * @return the parsed protocol metadata
   * @throws IllegalArgumentException if the response is not an object
   */
  public static ThirdPartyProtocols from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("third-party protocols must be a JSON object");
    }
    Map<String, ThirdPartyProtocol> protocols = new LinkedHashMap<>();
    value
        .asObject()
        .entrySet()
        .forEach(
            entry -> {
              if (!entry.getValue().isObject()) {
                throw new IllegalArgumentException(
                    "third-party protocol " + entry.getKey() + " must be a JSON object");
              }
              protocols.put(entry.getKey(), ThirdPartyProtocol.from(entry.getValue()));
            });
    return new ThirdPartyProtocols(protocols, value);
  }
}
