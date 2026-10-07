package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Serializable model contract.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#api-standards">Matrix
 *     specification</a>
 */
public interface Serializable {
  /**
   * Serialize this object into a JsonValue.
   *
   * @return a JsonValue
   */
  JsonValue toJson();
}
