package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Third-party locations matching a lookup query.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3thirdpartylocation">Matrix
 *     specification</a>
 */
public record ThirdPartyLocations(List<ThirdPartyLocation> locations, JsonValue raw) {

  /** Copies locations to an immutable list. */
  public ThirdPartyLocations {
    locations = List.copyOf(locations);
  }

  /**
   * Parses an array of third-party locations.
   *
   * @param value the response JSON
   * @return the parsed locations
   * @throws IllegalArgumentException if the response is not an array
   */
  public static ThirdPartyLocations from(JsonValue value) {
    if (value == null || !value.isArray()) {
      throw new IllegalArgumentException("third-party locations must be a JSON array");
    }
    JsonArray array = value.asArray();
    List<ThirdPartyLocation> locations = new ArrayList<>();
    for (int index = 0; index < array.size(); index++) {
      locations.add(ThirdPartyLocation.from(array.get(index)));
    }
    return new ThirdPartyLocations(locations, value);
  }
}
