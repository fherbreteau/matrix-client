package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth authentication and account-management metadata exposed by a homeserver.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1auth_metadata">Matrix
 *     specification</a>
 */
public record AuthMetadata(JsonValue raw) {

  /** Returns an OAuth metadata field by name, or {@code null} when absent. */
  public JsonValue get(String name) {
    return raw.asObject().get(name);
  }

  /** Returns the OAuth metadata fields, including unknown fields. */
  public Map<String, JsonValue> fields() {
    var fields = new LinkedHashMap<String, JsonValue>();
    raw.asObject().entrySet().forEach(entry -> fields.put(entry.getKey(), entry.getValue()));
    return Collections.unmodifiableMap(fields);
  }

  /** Parses an authentication metadata response. */
  public static AuthMetadata from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("Authentication metadata must be a JSON object");
    }
    return new AuthMetadata(value);
  }
}
